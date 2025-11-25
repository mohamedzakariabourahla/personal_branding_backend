package saas.personal_branding.api.auth.presentation.dto.response;

public record TokenResponse(String accessToken,
                            String tokenType,
                            String deviceId,
                            String deviceName) {
}
