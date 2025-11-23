package saas.personal_branding.api.infrastructure.mail;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.EmailVerificationNotifier;
import saas.personal_branding.api.application.service.PasswordResetNotifier;

@Component
public class AsyncMailNotifier implements PasswordResetNotifier, EmailVerificationNotifier {

    private final MailService mailService;

    public AsyncMailNotifier(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        mailService.sendPasswordResetEmail(to, resetLink);
    }

    @Override
    public void sendEmailVerificationEmail(String email, String verificationLink, String token) {
        mailService.sendEmailVerificationEmail(email, verificationLink, token);
    }
}
