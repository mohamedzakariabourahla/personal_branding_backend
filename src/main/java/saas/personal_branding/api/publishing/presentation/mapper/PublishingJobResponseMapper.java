package saas.personal_branding.api.publishing.presentation.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.FailureReasonNormalizer;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.presentation.dto.response.PublishingJobResponse;

@Component
public class PublishingJobResponseMapper {

    private final FailureReasonNormalizer failureReasonNormalizer;

    public PublishingJobResponseMapper(FailureReasonNormalizer failureReasonNormalizer) {
        this.failureReasonNormalizer = failureReasonNormalizer;
    }

    public PublishingJobResponse toResponse(PublishingJob job) {
        return new PublishingJobResponse(
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
                failureReasonNormalizer.toUserMessage(job.getFailureReason()),
                job.getExternalPostId()
        );
    }

    public List<PublishingJobResponse> toResponseList(List<PublishingJob> jobs) {
        return jobs.stream().map(this::toResponse).toList();
    }
}
