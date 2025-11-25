package saas.personal_branding.api.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class FailureReasonNormalizer {

    private final ObjectMapper objectMapper;

    public FailureReasonNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toUserMessage(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }

        String message = failureReason;
        try {
            record ProviderError(String message, String error_user_msg) { }
            record ErrorEnvelope(ProviderError error, String message) { }

            ErrorEnvelope parsed = objectMapper.readValue(failureReason, ErrorEnvelope.class);
            if (parsed.error() != null) {
                if (parsed.error().error_user_msg() != null && !parsed.error().error_user_msg().isBlank()) {
                    message = parsed.error().error_user_msg();
                } else if (parsed.error().message() != null && !parsed.error().message().isBlank()) {
                    message = parsed.error().message();
                }
            } else if (parsed.message() != null && !parsed.message().isBlank()) {
                message = parsed.message();
            }
        } catch (Exception ignored) {
            // fall back to the raw string when parsing fails
        }

        String lower = message.toLowerCase();
        if (lower.contains("only photo or video")) {
            return "The platform only accepts photos or videos. Upload a supported media file and try again.";
        }
        if (lower.contains("could not be fetched from this uri") || lower.contains("media download has failed")) {
            return "The media URL could not be fetched. Ensure the link is public, reachable, and in a supported format, then retry.";
        }
        if (lower.contains("oauth") && lower.contains("token")) {
            return "Authorization expired. Please reconnect the account and retry.";
        }
        if (message.length() > 220) {
            return message.substring(0, 220) + "...";
        }
        return message;
    }
}
