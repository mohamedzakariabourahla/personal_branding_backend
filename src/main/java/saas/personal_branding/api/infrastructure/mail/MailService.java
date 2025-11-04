package saas.personal_branding.api.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import saas.personal_branding.api.application.service.PasswordResetNotifier;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class MailService implements PasswordResetNotifier {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       ResourceLoader resourceLoader,
                       @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Reset your PersonalBranding password");

            String htmlTemplate = loadTemplate("mail/password-reset.html");
            String textTemplate = loadTemplate("mail/password-reset.txt");

            helper.setText(
                    textTemplate.replace("{{resetLink}}", resetLink),
                    htmlTemplate.replace("{{resetLink}}", resetLink)
            );

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Failed to send password reset email", e);
        }
    }

    private String loadTemplate(String location) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + location);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
