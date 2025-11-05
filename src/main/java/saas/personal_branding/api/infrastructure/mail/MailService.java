package saas.personal_branding.api.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MailService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       ResourceLoader resourceLoader,
                       @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("app.mail.from must be configured");
        }
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        sendTemplatedEmail(
                to,
                "Reset your PersonalBranding password",
                "mail/password-reset",
                Map.of("resetLink", resetLink)
        );
    }

    public void sendEmailVerificationEmail(String email, String verificationLink, String token) {
        sendTemplatedEmail(
                email,
                "Verify your PersonalBranding email",
                "mail/email-verification",
                Map.of(
                        "verificationLink", verificationLink,
                        "verificationToken", token
                )
        );
    }

    private void sendTemplatedEmail(String to, String subject, String templateBase, Map<String, String> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            String textTemplate = loadTemplate(templateBase + ".txt");
            String htmlTemplate = loadTemplate(templateBase + ".html");

            if (textTemplate == null && htmlTemplate == null) {
                throw new IllegalStateException("Mail template not found: " + templateBase);
            }

            String baselineTemplate = textTemplate != null ? textTemplate : htmlTemplate;
            String textBody = applyVariables(baselineTemplate, variables);
            String htmlBody = htmlTemplate != null ? applyVariables(htmlTemplate, variables) : textBody;

            helper.setText(textBody, htmlBody);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }

    private String loadTemplate(String location) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + location);
        if (!resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String applyVariables(String template, Map<String, String> variables) {
        String result = template;
        if (result == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
