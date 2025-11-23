package saas.personal_branding.api.infrastructure.mail;

import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import saas.personal_branding.api.application.exception.UserException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableConfigurationProperties(MailService.ProviderProperties.class)
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ResourceLoader resourceLoader;
    private final String fromAddress;
    private final RestClient resendClient;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public MailService(ResourceLoader resourceLoader,
                       @Value("${app.mail.from}") String fromAddress,
                       ProviderProperties providerProperties) {
        this.resourceLoader = resourceLoader;
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("app.mail.from must be configured");
        }
        this.fromAddress = fromAddress;
        this.resendClient = RestClient.builder()
                .baseUrl(providerProperties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + providerProperties.apiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
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
            String textTemplate = loadTemplate(templateBase + ".txt");
            String htmlTemplate = loadTemplate(templateBase + ".html");

            if (textTemplate == null && htmlTemplate == null) {
                throw new IllegalStateException("Mail template not found: " + templateBase);
            }

            String baselineTemplate = textTemplate != null ? textTemplate : htmlTemplate;
            String textBody = applyVariables(baselineTemplate, variables);
            String htmlBody = htmlTemplate != null ? applyVariables(htmlTemplate, variables) : textBody;

            resendClient.post()
                    .uri("/emails")
                    .body(Map.of(
                            "from", fromAddress,
                            "to", to,
                            "subject", subject,
                            "text", textBody,
                            "html", htmlBody
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Mail provider rejected request status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new UserException.EmailDispatchFailedException("Mail rejected: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("Mail provider communication failure", ex);
            throw new UserException.EmailDispatchFailedException("Mail provider unavailable. Please try again.");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }

    private String loadTemplate(String location) throws IOException {
        String cached = templateCache.get(location);
        if (cached != null) {
            return cached;
        }

        Resource resource = resourceLoader.getResource("classpath:" + location);
        if (!resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String template = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            templateCache.put(location, template);
            return template;
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

    @Validated
    @ConfigurationProperties(prefix = "app.mail.resend")
    public record ProviderProperties(
            @NotBlank String apiKey,
            @NotBlank String baseUrl
    ) {
    }
}
