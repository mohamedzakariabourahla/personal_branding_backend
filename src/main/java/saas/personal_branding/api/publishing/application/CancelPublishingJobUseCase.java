package saas.personal_branding.api.publishing.application;

import java.util.Optional;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.publishing.domain.PublishingJob;

@Component
public class CancelPublishingJobUseCase {

    private final PublishingJobService publishingJobService;

    public CancelPublishingJobUseCase(PublishingJobService publishingJobService) {
        this.publishingJobService = publishingJobService;
    }

    public boolean cancelForUser(Long userId, Long jobId, String reason) {
        Optional<PublishingJob> job = publishingJobService.find(jobId);
        if (job.isEmpty() || !job.get().getUserId().equals(userId)) {
            return false;
        }
        publishingJobService.cancel(jobId, reason);
        return true;
    }
}
