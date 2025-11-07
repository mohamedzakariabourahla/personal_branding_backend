package saas.personal_branding.api.application.service.platform;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.port.out.meta.MetaGraphClient;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformConnectionStatus;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.platform.PlatformAuthContext;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.infrastructure.provider.meta.MetaOAuthProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MetaOAuthApplicationService {

    private final PlatformOAuthStateRepository oauthStateRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final PlatformCredentialService platformCredentialService;
    private final MetaGraphClient metaGraphClient;
    private final MetaOAuthProperties properties;

    public MetaOAuthApplicationService(PlatformOAuthStateRepository oauthStateRepository,
                                       ReferenceDataRepository referenceDataRepository,
                                       PlatformCredentialService platformCredentialService,
                                       MetaGraphClient metaGraphClient,
                                       MetaOAuthProperties properties) {
        this.oauthStateRepository = oauthStateRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.platformCredentialService = platformCredentialService;
        this.metaGraphClient = metaGraphClient;
        this.properties = properties;
    }

    public OAuthAuthorizationContext startAuthorization(Long userId) {
        Platform platform = resolvePlatform();
        String state = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(properties.getStateTtl());

        PlatformOAuthState oauthState = PlatformOAuthState.builder()
                .state(state)
                .userId(userId)
                .platform(platform)
                .redirectUri(properties.getRedirectUri())
                .requestedScopes(properties.getScopes())
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        oauthStateRepository.save(oauthState);

        String authorizationUrl = buildAuthorizationUrl(state);
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

        MetaGraphClient.MetaAccessToken shortToken = metaGraphClient.exchangeCodeForUserToken(code, oauthState.getRedirectUri());
        MetaGraphClient.MetaAccessToken longToken = metaGraphClient.exchangeForLongLivedUserToken(shortToken.accessToken());

        List<MetaGraphClient.MetaPage> pages = metaGraphClient.fetchPages(longToken.accessToken());
        MetaGraphClient.MetaPage pageWithInstagram = pages.stream()
                .filter(page -> page.instagramAccount() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Instagram business account linked to Meta user"));

        MetaGraphClient.MetaInstagramAccount igAccount = pageWithInstagram.instagramAccount();
        Platform platform = oauthState.getPlatform();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("facebookPageId", pageWithInstagram.id());
        metadata.put("facebookPageName", pageWithInstagram.name());
        metadata.put("instagramId", igAccount.id());
        metadata.put("instagramUsername", igAccount.username());
        metadata.put("instagramName", igAccount.name());

        Optional<PlatformConnection> existing = platformCredentialService
                .findConnection(userId, platform.getId(), igAccount.id());

        PlatformConnection connection = PlatformConnection.builder()
                .id(existing.map(PlatformConnection::getId).orElse(null))
                .userId(userId)
                .platform(platform)
                .externalAccountId(igAccount.id())
                .externalUsername(igAccount.username())
                .externalDisplayName(igAccount.name())
                .accountMetadata(metadata)
                .status(PlatformConnectionStatus.CONNECTED)
                .build();

        PlatformConnection saved = platformCredentialService.saveConnection(connection);
        PlatformAuthContext authContext = PlatformAuthContext.builder()
                .accessToken(pageWithInstagram.accessToken())
                .refreshToken(longToken.accessToken())
                .tokenType("Bearer")
                .scopes(properties.getScopes())
                .accessTokenExpiresAt(null)
                .refreshTokenExpiresAt(longToken.expiresInSeconds() == null ? null : Instant.now().plusSeconds(longToken.expiresInSeconds()))
                .build();
        platformCredentialService.saveTokens(saved.getId(), authContext);

        oauthStateRepository.deleteByState(state);
        return saved;
    }

    private Platform resolvePlatform() {
        return referenceDataRepository.findPlatformByName(properties.getPlatformName())
                .orElseThrow(() -> new IllegalStateException("Platform not found: " + properties.getPlatformName()));
    }

    private String buildAuthorizationUrl(String state) {
        String scopeParam = String.join(",", properties.getScopes());
        return org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(properties.getAuthBaseUrl())
                .pathSegment(properties.getApiVersion(), "dialog", "oauth")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", scopeParam)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }
}
