package saas.personal_branding.api.application.service.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.port.out.meta.MetaGraphClient;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.publishing.infrastructure.provider.meta.MetaOAuthProperties;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaOAuthApplicationServiceTest {

    @Mock
    private PlatformOAuthStateRepository oauthStateRepository;

    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @Mock
    private PlatformCredentialService credentialService;

    @Mock
    private MetaGraphClient metaGraphClient;

    private MetaOAuthApplicationService service;

    private MetaOAuthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MetaOAuthProperties();
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://localhost:3000/meta/callback");
        properties.setPlatformName("Instagram");

        service = new MetaOAuthApplicationService(
                oauthStateRepository,
                referenceDataRepository,
                credentialService,
                metaGraphClient,
                properties
        );
    }

    @Test
    void throwsSelectionRequiredWhenMultiplePagesAndNoSelectionProvided() {
        Instant now = Instant.now();
        Platform platform = Platform.builder().id(1L).name("Instagram").build();
        PlatformOAuthState oauthState = PlatformOAuthState.builder()
                .state("state-123")
                .userId(42L)
                .platform(platform)
                .redirectUri(properties.getRedirectUri())
                .requestedScope("pages_show_list")
                .requestedScope("instagram_basic")
                .createdAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        when(oauthStateRepository.findByState("state-123")).thenReturn(Optional.of(oauthState));
        MetaGraphClient.MetaAccessToken shortToken = new MetaGraphClient.MetaAccessToken("short", "Bearer", 60L);
        MetaGraphClient.MetaAccessToken longToken = new MetaGraphClient.MetaAccessToken("long", "Bearer", 600L);
        when(metaGraphClient.exchangeCodeForUserToken("code-abc", properties.getRedirectUri())).thenReturn(shortToken);
        when(metaGraphClient.exchangeForLongLivedUserToken("short")).thenReturn(longToken);

        List<MetaGraphClient.MetaPage> pages = List.of(
                new MetaGraphClient.MetaPage(
                        "page-1",
                        "Alpha Realty",
                        "token-1",
                        new MetaGraphClient.MetaInstagramAccount("ig-1", "alpharealty", "Alpha Realty")
                ),
                new MetaGraphClient.MetaPage(
                        "page-2",
                        "Beta Realty",
                        "token-2",
                        new MetaGraphClient.MetaInstagramAccount("ig-2", "betarealty", "Beta Realty")
                )
        );
        when(metaGraphClient.fetchPages("long")).thenReturn(pages);

        assertThrows(
                PlatformException.SelectionRequiredException.class,
                () -> service.completeAuthorization(42L, "state-123", "code-abc", null),
                "Expected a selection-required exception when multiple IG pages are available"
        );
    }
}
