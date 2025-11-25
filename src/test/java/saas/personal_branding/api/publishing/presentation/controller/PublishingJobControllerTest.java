package saas.personal_branding.api.publishing.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.publishing.application.CancelPublishingJobUseCase;
import saas.personal_branding.api.publishing.application.ListPublishingAttemptsUseCase;
import saas.personal_branding.api.publishing.application.ListPublishingJobsUseCase;
import saas.personal_branding.api.publishing.application.RetryPublishingJobUseCase;
import saas.personal_branding.api.publishing.application.SchedulePublishingJobUseCase;
import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.domain.PublishingAttemptStatus;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.domain.PublishingJobStatus;
import saas.personal_branding.api.publishing.presentation.mapper.PublishingAttemptResponseMapper;
import saas.personal_branding.api.publishing.presentation.mapper.PublishingJobResponseMapper;
import io.micrometer.core.instrument.MeterRegistry;

@WebMvcTest(
        controllers = PublishingJobController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PublishingJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SchedulePublishingJobUseCase schedulePublishingJobUseCase;
    @MockBean
    private ListPublishingJobsUseCase listPublishingJobsUseCase;
    @MockBean
    private ListPublishingAttemptsUseCase listPublishingAttemptsUseCase;
    @MockBean
    private RetryPublishingJobUseCase retryPublishingJobUseCase;
    @MockBean
    private CancelPublishingJobUseCase cancelPublishingJobUseCase;
    @MockBean
    private AuthenticatedUserProvider authenticatedUserProvider;
    @MockBean
    private java.time.Clock clock;
    @MockBean
    private MeterRegistry meterRegistry;
    @MockBean
    private PublishingJobResponseMapper publishingJobResponseMapper;
    @MockBean
    private PublishingAttemptResponseMapper publishingAttemptResponseMapper;

    @Test
    void scheduleReturnsCreatedJob() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(10L);
        PublishingJob job = PublishingJob.builder()
                .id(111L)
                .userId(10L)
                .platformId(2L)
                .connectionId(3L)
                .mediaAssetIds(List.of("asset-1"))
                .caption("hello")
                .scheduledAt(Instant.parse("2025-01-01T00:00:00Z"))
                .createdAt(Instant.parse("2024-12-31T23:59:00Z"))
                .status(PublishingJobStatus.SCHEDULED)
                .attemptCount(0)
                .build();
        when(schedulePublishingJobUseCase.schedule(any())).thenReturn(job);
        when(publishingJobResponseMapper.toResponse(job)).thenReturn(
                new saas.personal_branding.api.publishing.presentation.dto.response.PublishingJobResponse(
                        job.getId(),
                        job.getPlatformId(),
                        job.getConnectionId(),
                        job.getMediaAssetIds(),
                        job.getCaption(),
                        job.getScheduledAt(),
                        job.getCreatedAt(),
                        job.getLastTriedAt(),
                        job.getCompletedAt(),
                        job.getAttemptCount(),
                        job.getStatus(),
                        job.getFailureReason(),
                        null,
                        job.getExternalPostId()
                )
        );

        mockMvc.perform(post("/api/publishing/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformId":2,
                                  "connectionId":3,
                                  "mediaAssetIds":["asset-1"],
                                  "caption":"hello",
                                  "scheduledAt":"2025-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(111))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.caption").value("hello"));
    }

    @Test
    void listAttemptsReturnsData() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(10L);
        PublishingAttempt attempt = PublishingAttempt.builder()
                .id(5L)
                .jobId(9L)
                .attemptedAt(Instant.parse("2024-12-31T00:00:00Z"))
                .status(PublishingAttemptStatus.FAILURE)
                .error("Oops")
                .providerResponse("detail")
                .build();
        when(listPublishingAttemptsUseCase.findForUser(any(), any(), any(int.class))).thenReturn(java.util.Optional.of(List.of(attempt)));
        when(publishingAttemptResponseMapper.toResponseList(any())).thenReturn(
                List.of(new saas.personal_branding.api.publishing.presentation.dto.response.PublishingAttemptResponse(
                        attempt.getId(),
                        attempt.getJobId(),
                        attempt.getAttemptedAt(),
                        attempt.getStatus().name(),
                        attempt.getError(),
                        attempt.getProviderResponse()
                ))
        );

        mockMvc.perform(get("/api/publishing/jobs/9/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILURE"))
                .andExpect(jsonPath("$[0].error").value("Oops"));
    }
}
