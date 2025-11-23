package saas.personal_branding.api.domain.scheduling;

import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PublishingAttempt {
    private final Long id;
    private final Long jobId;
    private final Instant attemptedAt;
    private final PublishingAttemptStatus status;
    private final String error;
    private final String providerResponse;
}
