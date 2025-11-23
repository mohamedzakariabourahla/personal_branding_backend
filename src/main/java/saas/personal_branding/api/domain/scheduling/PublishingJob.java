package saas.personal_branding.api.domain.scheduling;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PublishingJob {

    private final Long id;
    private final Long userId;
    private final Long platformId;
    private final Long connectionId;
    private final List<String> mediaAssetIds;
    private final String caption;
    private final Instant scheduledAt;
    private final Instant createdAt;
    private final Instant lastTriedAt;
    private final Instant completedAt;
    private final int attemptCount;
    private final PublishingJobStatus status;
    private final String failureReason;
    private final String externalPostId;
}
