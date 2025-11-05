package saas.personal_branding.api.infrastructure.mail;

public record EmailVerificationEmailRequestedEvent(String email, String verificationLink, String token) {
}
