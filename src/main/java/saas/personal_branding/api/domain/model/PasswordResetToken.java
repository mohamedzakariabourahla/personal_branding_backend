package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PasswordResetToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant usedAt;

    public boolean isExpired(Instant referenceTime) {
        return expiresAt.isBefore(referenceTime);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
