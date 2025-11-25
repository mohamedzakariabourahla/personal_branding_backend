package saas.personal_branding.api.publishing.presentation.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.publishing.application.CancelPublishingJobUseCase;
import saas.personal_branding.api.publishing.application.ListPublishingAttemptsUseCase;
import saas.personal_branding.api.publishing.application.ListPublishingJobsUseCase;
import saas.personal_branding.api.publishing.application.RetryPublishingJobUseCase;
import saas.personal_branding.api.publishing.application.SchedulePublishingJobUseCase;
import saas.personal_branding.api.publishing.presentation.dto.request.PublishingJobRequest;
import saas.personal_branding.api.publishing.presentation.dto.response.PublishingAttemptResponse;
import saas.personal_branding.api.publishing.presentation.dto.response.PublishingJobResponse;
import saas.personal_branding.api.publishing.presentation.mapper.PublishingAttemptResponseMapper;
import saas.personal_branding.api.publishing.presentation.mapper.PublishingJobResponseMapper;

@RestController
@RequestMapping("/api/publishing/jobs")
public class PublishingJobController {

    private final ListPublishingJobsUseCase listPublishingJobsUseCase;
    private final ListPublishingAttemptsUseCase listPublishingAttemptsUseCase;
    private final RetryPublishingJobUseCase retryPublishingJobUseCase;
    private final CancelPublishingJobUseCase cancelPublishingJobUseCase;
    private final SchedulePublishingJobUseCase schedulePublishingJobUseCase;
    private final PublishingJobResponseMapper publishingJobResponseMapper;
    private final PublishingAttemptResponseMapper publishingAttemptResponseMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PublishingJobController(ListPublishingJobsUseCase listPublishingJobsUseCase,
                                   ListPublishingAttemptsUseCase listPublishingAttemptsUseCase,
                                   RetryPublishingJobUseCase retryPublishingJobUseCase,
                                   CancelPublishingJobUseCase cancelPublishingJobUseCase,
                                   SchedulePublishingJobUseCase schedulePublishingJobUseCase,
                                   PublishingJobResponseMapper publishingJobResponseMapper,
                                   PublishingAttemptResponseMapper publishingAttemptResponseMapper,
                                   AuthenticatedUserProvider authenticatedUserProvider) {
        this.listPublishingJobsUseCase = listPublishingJobsUseCase;
        this.listPublishingAttemptsUseCase = listPublishingAttemptsUseCase;
        this.retryPublishingJobUseCase = retryPublishingJobUseCase;
        this.cancelPublishingJobUseCase = cancelPublishingJobUseCase;
        this.schedulePublishingJobUseCase = schedulePublishingJobUseCase;
        this.publishingJobResponseMapper = publishingJobResponseMapper;
        this.publishingAttemptResponseMapper = publishingAttemptResponseMapper;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<PublishingJobResponse>> listRecent() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        return ResponseEntity.ok(publishingJobResponseMapper.toResponseList(
                listPublishingJobsUseCase.listRecentForUser(userId, 50)
        ));
    }

    @PostMapping
    public ResponseEntity<PublishingJobResponse> schedule(@Valid @RequestBody PublishingJobRequest request) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var job = schedulePublishingJobUseCase.schedule(new SchedulePublishingJobUseCase.ScheduleCommand(
                userId,
                request.getPlatformId(),
                request.getConnectionId(),
                request.getMediaAssetIds(),
                request.getCaption(),
                request.getScheduledAt()
        ));

        PublishingJobResponse response = publishingJobResponseMapper.toResponse(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        boolean accepted = retryPublishingJobUseCase.retryForUser(userId, jobId);
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{jobId}/attempts")
    public ResponseEntity<List<PublishingAttemptResponse>> listAttempts(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        var attempts = listPublishingAttemptsUseCase.findForUser(userId, jobId, 20);
        if (attempts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<PublishingAttemptResponse> response = publishingAttemptResponseMapper.toResponseList(attempts.get());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancel(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        boolean cancelled = cancelPublishingJobUseCase.cancelForUser(userId, jobId, "Cancelled by user");
        if (!cancelled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }
}
