package saas.personal_branding.api.publishing.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PublishingJobRepository {

    PublishingJob save(PublishingJob job);

    Optional<PublishingJob> findById(Long id);

    List<PublishingJob> findByIds(List<Long> ids);

    PublishingJob update(PublishingJob job);

    List<PublishingJob> findDue(Instant now, int max);

    List<PublishingJob> findRecentByUserId(Long userId, int limit);
}
