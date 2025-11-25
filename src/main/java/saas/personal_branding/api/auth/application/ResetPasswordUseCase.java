package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PasswordResetService;

@Component
public class ResetPasswordUseCase {

    private final PasswordResetService passwordResetService;

    public ResetPasswordUseCase(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    public void execute(String token, String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
    }
}
