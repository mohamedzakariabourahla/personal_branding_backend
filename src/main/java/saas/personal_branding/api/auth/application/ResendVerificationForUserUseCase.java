package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.EmailVerificationService;

@Component
public class ResendVerificationForUserUseCase {

    private final EmailVerificationService emailVerificationService;

    public ResendVerificationForUserUseCase(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    public void execute(Long userId) {
        emailVerificationService.resendVerification(userId);
    }
}
