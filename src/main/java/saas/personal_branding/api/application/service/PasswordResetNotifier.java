package saas.personal_branding.api.application.service;

public interface PasswordResetNotifier {
    void sendPasswordResetEmail(String email, String resetLink);
}
