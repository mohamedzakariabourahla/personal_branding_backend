package saas.personal_branding.api.presentation.dto.response;

public record AuthResponse(UserResponse user, TokenResponse tokens) {
}
