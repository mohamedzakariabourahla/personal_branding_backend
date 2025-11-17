package saas.personal_branding.api.application.service.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.port.out.youtube.YouTubeApiClient;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformConnectionStatus;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.platform.PlatformAuthContext;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.infrastructure.provider.youtube.YouTubeOAuthProperties;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class YouTubeOAuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeOAuthApplicationService.class);

    private final PlatformOAuthStateRepository oauthStateRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final PlatformCredentialService credentialService;
    private final YouTubeApiClient youTubeApiClient;
    private final YouTubeOAuthProperties properties;

    public YouTubeOAuthApplicationService(PlatformOAuthStateRepository oauthStateRepository,
                                          ReferenceDataRepository referenceDataRepository,
                                          PlatformCredentialService credentialService,
                                          YouTubeApiClient youTubeApiClient,
                                          YouTubeOAuthProperties properties) {
        this.oauthStateRepository = oauthStateRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.credentialService = credentialService;
        this.youTubeApiClient = youTubeApiClient;
        this.properties = properties;
    }

    public OAuthAuthorizationContext startAuthorization(Long userId) {
        Platform platform = resolvePlatform();
        String state = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(properties.getStateTtl());
        String codeVerifier = generateCodeVerifier();
        log.info("YouTube OAuth start: userId={} platform={} state={}", userId, platform.getName(), state);

        PlatformOAuthState oauthState = PlatformOAuthState.builder()
                .state(state)
                .userId(userId)
                .platform(platform)
                .codeVerifier(codeVerifier)
                .redirectUri(properties.getRedirectUri())
                .requestedScopes(properties.getScopes())
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        oauthStateRepository.save(oauthState);

        String authorizationUrl = buildAuthorizationUrl(state, codeVerifier);
        return new OAuthAuthorizationContext(authorizationUrl, state, expiresAt);
    }

    public PlatformConnection completeAuthorization(Long userId, String state, String code, String requestedChannelId) {
        PlatformOAuthState oauthState = oauthStateRepository.findByState(state)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OAuth state"));
        if (!oauthState.getUserId().equals(userId)) {
            throw new IllegalArgumentException("OAuth state does not belong to current user");
        }
        if (oauthState.getExpiresAt().isBefore(Instant.now())) {
            oauthStateRepository.deleteByState(state);
            throw new IllegalArgumentException("OAuth state expired");
        }

        log.info("YouTube OAuth completion started: userId={} state={}", userId, state);

        YouTubeApiClient.YouTubeTokens tokens;
        List<YouTubeApiClient.YouTubeChannel> channels;
        try {
            tokens = youTubeApiClient.exchangeCodeForTokens(code, oauthState.getRedirectUri(), oauthState.getCodeVerifier());
            channels = youTubeApiClient.fetchChannels(tokens.accessToken());
        } catch (RuntimeException ex) {
            log.error("YouTube OAuth provider failure: state={} userId={}", state, userId, ex);
            throw new PlatformException.ProviderCommunicationException("YouTube temporarily rejected the request. Retry in a moment.");
        }

        if (channels.isEmpty()) {
            log.warn("YouTube OAuth no channels: userId={} state={}", userId, state);
            throw new PlatformException.NoEligibleAccountException();
        }

        YouTubeApiClient.YouTubeChannel selectedChannel = resolveSelectedChannel(channels, requestedChannelId);
        Platform platform = oauthState.getPlatform();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("channelId", selectedChannel.id());
        metadata.put("channelTitle", selectedChannel.title());
        metadata.put("channelHandle", selectedChannel.handle());
        metadata.put("thumbnailUrl", selectedChannel.thumbnailUrl());

        Optional<PlatformConnection> existing = credentialService.findConnection(userId, platform.getId(), selectedChannel.id());

        PlatformConnection connection = PlatformConnection.builder()
                .id(existing.map(PlatformConnection::getId).orElse(null))
                .userId(userId)
                .platform(platform)
                .externalAccountId(selectedChannel.id())
                .externalUsername(selectedChannel.handle())
                .externalDisplayName(selectedChannel.title())
                .accountMetadata(metadata)
                .status(PlatformConnectionStatus.CONNECTED)
                .build();

        PlatformConnection saved = credentialService.saveConnection(connection);

        PlatformAuthContext authContext = PlatformAuthContext.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType() == null ? "Bearer" : tokens.tokenType())
                .scopes(oauthState.getRequestedScopes())
                .accessTokenExpiresAt(tokens.expiresInSeconds() == null ? null : Instant.now().plusSeconds(tokens.expiresInSeconds()))
                .refreshTokenExpiresAt(null)
                .build();
        credentialService.saveTokens(saved.getId(), authContext);

        oauthStateRepository.deleteByState(state);
        log.info("YouTube OAuth completed: userId={} connectionId={} channelId={}", userId, saved.getId(), selectedChannel.id());
        return saved;
    }

    private YouTubeApiClient.YouTubeChannel resolveSelectedChannel(List<YouTubeApiClient.YouTubeChannel> channels, String requestedChannelId) {
        if (requestedChannelId != null && !requestedChannelId.isBlank()) {
            return channels.stream()
                    .filter(channel -> channel.id().equals(requestedChannelId))
                    .findFirst()
                    .orElseThrow(() -> new PlatformException.InvalidAccountSelectionException(requestedChannelId));
        }

        if (channels.size() == 1) {
            return channels.get(0);
        }

        List<PlatformException.PlatformSelectionCandidate> candidates = channels.stream()
                .map(channel -> new PlatformException.PlatformSelectionCandidate(
                        channel.id(),
                        channel.title(),
                        null,
                        channel.handle(),
                        null
                ))
                .toList();
        throw new PlatformException.SelectionRequiredException(candidates);
    }

    private Platform resolvePlatform() {
        return referenceDataRepository.findPlatformByName(properties.getPlatformName())
                .orElseThrow(() -> new IllegalStateException("Platform not found: " + properties.getPlatformName()));
    }

    private String buildAuthorizationUrl(String state, String codeVerifier) {
        String scopeParam = String.join(" ", properties.getScopes());
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return UriComponentsBuilder.fromHttpUrl(properties.getAuthBaseUrl())
                .path("/o/oauth2/v2/auth")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", scopeParam)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
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
}
