package saas.personal_branding.api.publishing.domain;

import java.util.List;

public interface PublishingAttemptRepository {

    PublishingAttempt save(PublishingAttempt attempt);

    List<PublishingAttempt> findByJobId(Long jobId, int limit);
}
