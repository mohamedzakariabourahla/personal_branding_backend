package saas.personal_branding.api.infrastructure.mail;

public record PasswordResetEmailRequestedEvent(String email, String resetLink) {
}
