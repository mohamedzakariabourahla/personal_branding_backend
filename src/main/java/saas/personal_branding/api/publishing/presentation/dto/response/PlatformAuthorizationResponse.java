package saas.personal_branding.api.publishing.presentation.dto.response;

import java.time.Instant;

public record PlatformAuthorizationResponse(String authorizationUrl, String state, Instant expiresAt) {
}
