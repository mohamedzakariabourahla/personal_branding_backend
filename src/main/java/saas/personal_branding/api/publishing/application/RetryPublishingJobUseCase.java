package saas.personal_branding.api.publishing.application;

import java.util.Optional;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.publishing.domain.PublishingJob;

@Component
public class RetryPublishingJobUseCase {

    private final PublishingJobService publishingJobService;

    public RetryPublishingJobUseCase(PublishingJobService publishingJobService) {
        this.publishingJobService = publishingJobService;
    }

    public boolean retryForUser(Long userId, Long jobId) {
        Optional<PublishingJob> job = publishingJobService.find(jobId);
        if (job.isEmpty() || !job.get().getUserId().equals(userId)) {
            return false;
        }
        publishingJobService.retryNow(jobId);
        return true;
    }
}
