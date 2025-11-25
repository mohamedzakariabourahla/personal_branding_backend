package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.EmailVerificationService;

@Component
public class VerifyEmailUseCase {

    private final EmailVerificationService emailVerificationService;

    public VerifyEmailUseCase(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    public void execute(String token) {
        emailVerificationService.verifyToken(token);
    }
}
