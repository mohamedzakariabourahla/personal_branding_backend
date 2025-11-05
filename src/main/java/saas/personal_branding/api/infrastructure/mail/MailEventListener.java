package saas.personal_branding.api.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MailEventListener {

    private static final Logger log = LoggerFactory.getLogger(MailEventListener.class);

    private final MailService mailService;

    public MailEventListener(MailService mailService) {
        this.mailService = mailService;
    }

    @Async("mailTaskExecutor")
    @EventListener
    public void handlePasswordReset(PasswordResetEmailRequestedEvent event) {
        try {
            mailService.sendPasswordResetEmail(event.email(), event.resetLink());
        } catch (Exception ex) {
            log.error("Failed to dispatch password reset email to {}", event.email(), ex);
        }
    }

    @Async("mailTaskExecutor")
    @EventListener
    public void handleEmailVerification(EmailVerificationEmailRequestedEvent event) {
        try {
            mailService.sendEmailVerificationEmail(event.email(), event.verificationLink(), event.token());
        } catch (Exception ex) {
            log.error("Failed to dispatch verification email to {}", event.email(), ex);
        }
    }
}
