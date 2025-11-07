package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PlatformToken {
    private final Long id;
    private final Long connectionId;
    private final EncryptedSecret accessToken;
    private final EncryptedSecret refreshToken;
    private final String tokenType;
    @Singular
    private final List<String> scopes;
    private final Instant accessTokenExpiresAt;
    private final Instant refreshTokenExpiresAt;
    private final Instant lastRotatedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String fingerprint;
}
