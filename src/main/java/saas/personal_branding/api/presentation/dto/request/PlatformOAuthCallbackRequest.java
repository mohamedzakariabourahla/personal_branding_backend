package saas.personal_branding.api.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlatformOAuthCallbackRequest(
        @NotBlank String state,
        @NotBlank String code
) {
}
