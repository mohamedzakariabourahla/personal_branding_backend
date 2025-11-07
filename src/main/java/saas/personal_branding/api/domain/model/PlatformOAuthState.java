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
public class PlatformOAuthState {
    private final String state;
    private final Long userId;
    private final Platform platform;
    private final String codeVerifier;
    private final String redirectUri;
    @Singular
    private final List<String> requestedScopes;
    private final Instant createdAt;
    private final Instant expiresAt;
}
