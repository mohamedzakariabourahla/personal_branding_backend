package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.EmailVerificationService;

@Component
public class ResendVerificationForEmailUseCase {

    private final EmailVerificationService emailVerificationService;

    public ResendVerificationForEmailUseCase(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    public void execute(String email) {
        emailVerificationService.resendVerificationForEmail(email);
    }
}
