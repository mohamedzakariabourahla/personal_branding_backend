package saas.personal_branding.api.application.service.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.port.out.youtube.YouTubeApiClient;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.publishing.infrastructure.provider.youtube.YouTubeOAuthProperties;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YouTubeOAuthApplicationServiceTest {

    @Mock
    private PlatformOAuthStateRepository oauthStateRepository;

    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @Mock
    private PlatformCredentialService credentialService;

    @Mock
    private YouTubeApiClient youTubeApiClient;

    private YouTubeOAuthApplicationService service;

    private YouTubeOAuthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new YouTubeOAuthProperties();
        properties.setClientId("yt-client");
        properties.setClientSecret("yt-secret");
        properties.setRedirectUri("http://localhost:3000/youtube/callback");
        properties.setPlatformName("YouTube");

        service = new YouTubeOAuthApplicationService(
                oauthStateRepository,
                referenceDataRepository,
                credentialService,
                youTubeApiClient,
                properties
        );
    }

    @Test
    void throwsSelectionRequiredWhenMultipleChannelsAndNoSelectionProvided() {
        Instant now = Instant.now();
        Platform platform = Platform.builder().id(5L).name("YouTube").build();
        PlatformOAuthState state = PlatformOAuthState.builder()
                .state("yt-state")
                .userId(7L)
                .platform(platform)
                .codeVerifier("code-verifier")
                .redirectUri(properties.getRedirectUri())
                .requestedScope("youtube.readonly")
                .createdAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        when(oauthStateRepository.findByState("yt-state")).thenReturn(Optional.of(state));
        when(youTubeApiClient.exchangeCodeForTokens(anyString(), anyString(), anyString()))
                .thenReturn(new YouTubeApiClient.YouTubeTokens("access", "refresh", 3600L, "scope", "Bearer"));

        List<YouTubeApiClient.YouTubeChannel> channels = List.of(
                new YouTubeApiClient.YouTubeChannel("channel-1", "Brand One", "@brandone", null),
                new YouTubeApiClient.YouTubeChannel("channel-2", "Brand Two", "@brandtwo", null)
        );
        when(youTubeApiClient.fetchChannels("access")).thenReturn(channels);

        assertThrows(
                PlatformException.SelectionRequiredException.class,
                () -> service.completeAuthorization(7L, "yt-state", "auth-code", null)
        );
    }
}
