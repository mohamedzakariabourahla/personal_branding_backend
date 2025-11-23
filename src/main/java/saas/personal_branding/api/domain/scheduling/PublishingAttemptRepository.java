package saas.personal_branding.api.domain.scheduling;

import java.util.List;

public interface PublishingAttemptRepository {
    PublishingAttempt save(PublishingAttempt attempt);

    List<PublishingAttempt> findByJobId(Long jobId, int limit);
}
