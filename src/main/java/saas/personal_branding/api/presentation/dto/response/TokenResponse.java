package saas.personal_branding.api.presentation.dto.response;

import java.time.Instant;

public record TokenResponse(String accessToken,
                            String tokenType,
                            String deviceId,
                            String deviceName) {
}
