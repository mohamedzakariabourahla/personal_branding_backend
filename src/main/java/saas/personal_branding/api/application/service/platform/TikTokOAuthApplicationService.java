package saas.personal_branding.api.application.service.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.port.out.tiktok.TikTokApiClient;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformConnectionStatus;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.platform.PlatformAuthContext;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.publishing.infrastructure.provider.tiktok.TikTokOAuthProperties;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TikTokOAuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TikTokOAuthApplicationService.class);

    private final PlatformOAuthStateRepository oauthStateRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final PlatformCredentialService credentialService;
    private final TikTokApiClient tikTokApiClient;
    private final TikTokOAuthProperties properties;

    public TikTokOAuthApplicationService(PlatformOAuthStateRepository oauthStateRepository,
                                         ReferenceDataRepository referenceDataRepository,
                                         PlatformCredentialService credentialService,
                                         TikTokApiClient tikTokApiClient,
                                         TikTokOAuthProperties properties) {
        this.oauthStateRepository = oauthStateRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.credentialService = credentialService;
        this.tikTokApiClient = tikTokApiClient;
        this.properties = properties;
    }

    public OAuthAuthorizationContext startAuthorization(Long userId) {
        Platform platform = resolvePlatform();
        String state = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();
        Instant expiresAt = Instant.now().plus(properties.getStateTtl());

        PlatformOAuthState oauthState = PlatformOAuthState.builder()
                .state(state)
                .userId(userId)
                .platform(platform)
                .codeVerifier(codeVerifier)
                .redirectUri(properties.getRedirectUri())
                .requestedScopes(splitScopes(properties.getScopes()))
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        oauthStateRepository.save(oauthState);

        String authorizationUrl = buildAuthorizationUrl(state, codeVerifier);
        return new OAuthAuthorizationContext(authorizationUrl, state, expiresAt);
    }

    public PlatformConnection completeAuthorization(Long userId, String state, String code) {
        PlatformOAuthState oauthState = oauthStateRepository.findByState(state)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OAuth state"));
        if (!oauthState.getUserId().equals(userId)) {
            throw new IllegalArgumentException("OAuth state does not belong to current user");
        }
        if (oauthState.getExpiresAt().isBefore(Instant.now())) {
            oauthStateRepository.deleteByState(state);
            throw new IllegalArgumentException("OAuth state expired");
        }

        log.info("TikTok OAuth completion started: userId={} state={}", userId, state);

        TikTokApiClient.TikTokTokens tokens;
        TikTokApiClient.TikTokUser user;
        try {
            tokens = tikTokApiClient.exchangeCodeForTokens(code, oauthState.getRedirectUri(), oauthState.getCodeVerifier());
            user = tikTokApiClient.fetchUser(tokens.accessToken());
        } catch (RuntimeException ex) {
            log.error("TikTok OAuth provider failure: state={} userId={}", state, userId, ex);
            throw new PlatformException.ProviderCommunicationException("TikTok temporarily rejected the request. Retry in a moment.");
        }
        Platform platform = oauthState.getPlatform();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("openId", user.openId());
        metadata.put("displayName", user.displayName());
        metadata.put("avatarUrl", user.avatarUrl());

        Optional<PlatformConnection> existing = credentialService.findConnection(userId, platform.getId(), user.openId());

        PlatformConnection connection = PlatformConnection.builder()
                .id(existing.map(PlatformConnection::getId).orElse(null))
                .userId(userId)
                .platform(platform)
                .externalAccountId(user.openId())
                .externalUsername(user.displayName())
                .externalDisplayName(user.displayName())
                .accountMetadata(metadata)
                .status(PlatformConnectionStatus.CONNECTED)
                .build();

        PlatformConnection saved = credentialService.saveConnection(connection);

        PlatformAuthContext authContext = PlatformAuthContext.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .scopes(oauthState.getRequestedScopes())
                .accessTokenExpiresAt(tokens.expiresInSeconds() == null ? null : Instant.now().plusSeconds(tokens.expiresInSeconds()))
                .refreshTokenExpiresAt(tokens.refreshExpiresInSeconds() == null ? null : Instant.now().plusSeconds(tokens.refreshExpiresInSeconds()))
                .build();
        credentialService.saveTokens(saved.getId(), authContext);

        oauthStateRepository.deleteByState(state);
        log.info("TikTok OAuth completed: userId={} connectionId={} openId={}", userId, saved.getId(), user.openId());
        return saved;
    }

    private Platform resolvePlatform() {
        return referenceDataRepository.findPlatformByName(properties.getPlatformName())
                .orElseThrow(() -> new IllegalStateException("Platform not found: " + properties.getPlatformName()));
    }

    private String buildAuthorizationUrl(String state, String codeVerifier) {
        String scopeParam = String.join(",", splitScopes(properties.getScopes()));
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return UriComponentsBuilder.fromHttpUrl(properties.getAuthBaseUrl())
                .pathSegment("v2", "auth", "authorize")
                .queryParam("client_key", properties.getClientKey())
                .queryParam("scope", scopeParam)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();
    }

    private String generateCodeVerifier() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes());
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot generate code challenge", ex);
        }
    }

    private List<String> splitScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
