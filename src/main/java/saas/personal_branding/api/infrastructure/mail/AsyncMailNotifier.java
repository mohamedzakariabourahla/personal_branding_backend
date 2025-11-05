package saas.personal_branding.api.infrastructure.mail;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.EmailVerificationNotifier;
import saas.personal_branding.api.application.service.PasswordResetNotifier;

@Component
public class AsyncMailNotifier implements PasswordResetNotifier, EmailVerificationNotifier {

    private final ApplicationEventPublisher eventPublisher;

    public AsyncMailNotifier(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        eventPublisher.publishEvent(new PasswordResetEmailRequestedEvent(to, resetLink));
    }

    @Override
    public void sendEmailVerificationEmail(String email, String verificationLink, String token) {
        eventPublisher.publishEvent(new EmailVerificationEmailRequestedEvent(email, verificationLink, token));
    }
}
