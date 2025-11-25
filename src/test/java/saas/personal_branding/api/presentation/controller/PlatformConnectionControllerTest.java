package saas.personal_branding.api.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.application.service.platform.MetaOAuthApplicationService;
import saas.personal_branding.api.application.service.platform.TikTokOAuthApplicationService;
import saas.personal_branding.api.application.service.platform.YouTubeOAuthApplicationService;
import saas.personal_branding.api.infrastructure.security.ResponseStatusMetricsFilter;
import saas.personal_branding.api.publishing.presentation.controller.PlatformConnectionController;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(controllers = PlatformConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlatformConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformCredentialService platformCredentialService;

    @MockBean
    private MetaOAuthApplicationService metaOAuthApplicationService;

    @MockBean
    private TikTokOAuthApplicationService tikTokOAuthApplicationService;

    @MockBean
    private YouTubeOAuthApplicationService youtubeOAuthApplicationService;

    @MockBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @MockBean
    private ResponseStatusMetricsFilter responseStatusMetricsFilter;

    @Test
    @DisplayName("Meta completion responds with SELECTION_REQUIRED payload when multiple pages are returned")
    void returnsSelectionRequiredWhenMetaRequiresUserChoice() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(42L);
        List<PlatformException.PlatformSelectionCandidate> candidates = List.of(
                new PlatformException.PlatformSelectionCandidate(
                        "page-1",
                        "Alpha Realty",
                        "ig-1",
                        "alpharealty",
                        "Alpha Realty"
                ),
                new PlatformException.PlatformSelectionCandidate(
                        "page-2",
                        "Beta Realty",
                        "ig-2",
                        "betarealty",
                        "Beta Realty"
                )
        );

        when(metaOAuthApplicationService.completeAuthorization(anyLong(), anyString(), anyString(), nullable(String.class)))
                .thenThrow(new PlatformException.SelectionRequiredException(candidates));

        mockMvc.perform(
                        post("/api/platforms/meta/oauth/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"state":"state-123","code":"code-abc"}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SELECTION_REQUIRED"))
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates[0].primaryId").value("page-1"))
                .andExpect(jsonPath("$.candidates[0].secondaryId").value("ig-1"))
                .andExpect(jsonPath("$.candidates[1].primaryName").value("Beta Realty"));
}
}
