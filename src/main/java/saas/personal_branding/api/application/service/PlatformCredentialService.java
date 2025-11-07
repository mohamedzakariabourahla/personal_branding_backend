package saas.personal_branding.api.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.security.SecretCipher;
import saas.personal_branding.api.domain.model.EncryptedSecret;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformToken;
import saas.personal_branding.api.domain.platform.PlatformAuthContext;
import saas.personal_branding.api.domain.repository.PlatformCredentialRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlatformCredentialService {

    private final PlatformCredentialRepository credentialRepository;
    private final SecretCipher secretCipher;

    public PlatformCredentialService(PlatformCredentialRepository credentialRepository,
                                     SecretCipher secretCipher) {
        this.credentialRepository = credentialRepository;
        this.secretCipher = secretCipher;
    }

    public PlatformConnection saveConnection(PlatformConnection connection) {
        return credentialRepository.saveConnection(connection);
    }

    public List<PlatformConnection> findConnections(Long userId) {
        return credentialRepository.findConnectionsByUserId(userId);
    }

    public Optional<PlatformConnection> findConnectionById(Long connectionId) {
        return credentialRepository.findConnectionById(connectionId);
    }

    public Optional<PlatformConnection> findConnection(Long userId, Long platformId, String externalAccountId) {
        return credentialRepository.findConnection(userId, platformId, externalAccountId);
    }

    public void deleteConnection(Long connectionId) {
        credentialRepository.deleteConnection(connectionId);
    }

    public PlatformToken saveTokens(Long connectionId, PlatformAuthContext authContext) {
        var now = java.time.Instant.now();
        PlatformToken token = PlatformToken.builder()
                .connectionId(connectionId)
                .accessToken(encrypt(authContext.getAccessToken()))
                .refreshToken(encrypt(authContext.getRefreshToken()))
                .tokenType(authContext.getTokenType())
                .scopes(authContext.getScopes())
                .accessTokenExpiresAt(authContext.getAccessTokenExpiresAt())
                .refreshTokenExpiresAt(authContext.getRefreshTokenExpiresAt())
                .lastRotatedAt(now)
                .build();
        return credentialRepository.saveToken(token);
    }

    public Optional<PlatformAuthContext> loadAuthContext(Long connectionId) {
        return credentialRepository.findTokenByConnectionId(connectionId)
                .map(this::toAuthContext);
    }

    private EncryptedSecret encrypt(String plain) {
        return plain == null ? null : secretCipher.encrypt(plain);
    }

    private String decrypt(EncryptedSecret secret) {
        return secret == null ? null : secretCipher.decrypt(secret);
    }

    private PlatformAuthContext toAuthContext(PlatformToken token) {
        return PlatformAuthContext.builder()
                .accessToken(decrypt(token.getAccessToken()))
                .refreshToken(decrypt(token.getRefreshToken()))
                .tokenType(token.getTokenType())
                .scopes(token.getScopes())
                .accessTokenExpiresAt(token.getAccessTokenExpiresAt())
                .refreshTokenExpiresAt(token.getRefreshTokenExpiresAt())
                .build();
    }
}
