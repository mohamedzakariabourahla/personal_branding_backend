package saas.personal_branding.api.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;

@Component
public class SecurityContextAuthenticatedUserProvider implements AuthenticatedUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Authenticated user id is not available");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long principalId) {
            return principalId;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Authenticated user id is not available");
    }
}
