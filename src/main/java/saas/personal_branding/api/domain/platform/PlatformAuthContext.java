package saas.personal_branding.api.domain.platform;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(exclude = {"accessToken", "refreshToken"})
public class PlatformAuthContext {
    private final String accessToken;
    private final String refreshToken;
    private final Instant accessTokenExpiresAt;
    private final Instant refreshTokenExpiresAt;
    private final String tokenType;
    @Singular
    private final List<String> scopes;
}
