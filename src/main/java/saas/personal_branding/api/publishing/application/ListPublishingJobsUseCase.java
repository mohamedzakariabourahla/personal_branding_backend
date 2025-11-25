package saas.personal_branding.api.publishing.application;

import java.util.List;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.publishing.domain.PublishingJob;

@Component
public class ListPublishingJobsUseCase {

    private final PublishingJobService publishingJobService;

    public ListPublishingJobsUseCase(PublishingJobService publishingJobService) {
        this.publishingJobService = publishingJobService;
    }

    public List<PublishingJob> listRecentForUser(Long userId, int limit) {
        return publishingJobService.findRecent(userId, limit);
    }
}
