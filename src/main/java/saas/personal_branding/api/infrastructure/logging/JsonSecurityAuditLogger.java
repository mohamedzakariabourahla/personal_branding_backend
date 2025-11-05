package saas.personal_branding.api.infrastructure.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.SecurityAuditLogger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonSecurityAuditLogger implements SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("security-audit");

    private final ObjectMapper objectMapper;

    public JsonSecurityAuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void log(String action, Map<String, ?> details) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now());
        payload.put("action", action);
        if (details != null && !details.isEmpty()) {
            payload.put("details", details);
        }
        try {
            log.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize security audit event for action {}", action, ex);
        }
    }
}
