package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PasswordResetService;

@Component
public class RequestPasswordResetUseCase {

    private final PasswordResetService passwordResetService;

    public RequestPasswordResetUseCase(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    public PasswordResetService.RequestResetResult execute(String email) {
        return passwordResetService.requestPasswordReset(email);
    }
}
