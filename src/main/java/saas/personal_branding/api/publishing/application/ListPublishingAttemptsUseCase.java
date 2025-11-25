package saas.personal_branding.api.publishing.application;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.domain.PublishingJob;

@Component
public class ListPublishingAttemptsUseCase {

    private final PublishingJobService publishingJobService;

    public ListPublishingAttemptsUseCase(PublishingJobService publishingJobService) {
        this.publishingJobService = publishingJobService;
    }

    public Optional<List<PublishingAttempt>> findForUser(Long userId, Long jobId, int limit) {
        Optional<PublishingJob> job = publishingJobService.find(jobId);
        if (job.isEmpty() || !job.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(publishingJobService.findAttempts(jobId, limit));
    }
}
