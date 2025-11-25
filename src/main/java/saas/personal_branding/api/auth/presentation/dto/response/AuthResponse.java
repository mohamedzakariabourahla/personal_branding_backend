package saas.personal_branding.api.auth.presentation.dto.response;

import saas.personal_branding.api.user.presentation.dto.response.UserResponse;

public record AuthResponse(UserResponse user, TokenResponse tokens) {
}
