package saas.personal_branding.api.application.service;

import java.util.Map;

public interface SecurityAuditLogger {

    void log(String action, Map<String, ?> details);
}
