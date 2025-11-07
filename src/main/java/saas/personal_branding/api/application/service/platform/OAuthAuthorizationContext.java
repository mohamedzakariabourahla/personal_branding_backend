package saas.personal_branding.api.application.service.platform;

import java.time.Instant;

public record OAuthAuthorizationContext(String authorizationUrl, String state, Instant expiresAt) {
}
