package saas.personal_branding.api.presentation.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.domain.scheduling.PublishingJob;
import saas.personal_branding.api.presentation.dto.request.PublishingJobRequest;
import saas.personal_branding.api.presentation.dto.response.PublishingAttemptResponse;
import saas.personal_branding.api.presentation.dto.response.PublishingJobResponse;

@RestController
@RequestMapping("/api/publishing/jobs")
public class PublishingJobController {

    private final PublishingJobService publishingJobService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PublishingJobController(PublishingJobService publishingJobService,
                                   AuthenticatedUserProvider authenticatedUserProvider) {
        this.publishingJobService = publishingJobService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<PublishingJobResponse>> listRecent() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        List<PublishingJob> jobs = publishingJobService.findRecent(userId, 50);
        List<PublishingJobResponse> response = jobs.stream()
                .map(job -> new PublishingJobResponse(
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
                job.getExternalPostId()
        ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PublishingJobResponse> schedule(@Valid @RequestBody PublishingJobRequest request) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        PublishingJob job = publishingJobService.schedule(new PublishingJobService.ScheduleCommand(
                userId,
                request.getPlatformId(),
                request.getConnectionId(),
                request.getMediaAssetIds(),
                request.getCaption(),
                request.getScheduledAt()
        ));

        PublishingJobResponse response = new PublishingJobResponse(
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
                job.getExternalPostId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        PublishingJob job = publishingJobService.find(jobId)
                .filter(j -> j.getUserId().equals(userId))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        publishingJobService.retryNow(jobId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{jobId}/attempts")
    public ResponseEntity<List<PublishingAttemptResponse>> listAttempts(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        PublishingJob job = publishingJobService.find(jobId)
                .filter(j -> j.getUserId().equals(userId))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<PublishingAttemptResponse> response = publishingJobService.findAttempts(jobId, 20).stream()
                .map(a -> new PublishingAttemptResponse(
                        a.getId(),
                        a.getJobId(),
                        a.getAttemptedAt(),
                        a.getStatus().name(),
                        a.getError(),
                        a.getProviderResponse()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancel(@PathVariable Long jobId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        PublishingJob job = publishingJobService.find(jobId)
                .filter(j -> j.getUserId().equals(userId))
                .orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        publishingJobService.cancel(jobId, "Cancelled by user");
        return ResponseEntity.noContent().build();
    }
}
