package saas.personal_branding.api.application.service;

public interface EmailVerificationNotifier {
    void sendEmailVerificationEmail(String email, String verificationLink, String token);
}
