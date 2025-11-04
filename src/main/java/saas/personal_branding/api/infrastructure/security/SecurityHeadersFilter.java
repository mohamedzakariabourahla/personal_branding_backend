package saas.personal_branding.api.infrastructure.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final String contentSecurityPolicy;
    private final String frameAncestors;

    public SecurityHeadersFilter(
            @Value("${app.security.csp:default-src 'self'; img-src 'self' data:; font-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'}") String contentSecurityPolicy,
            @Value("${app.security.frame-ancestors:none}") String frameAncestors) {
        this.contentSecurityPolicy = contentSecurityPolicy;
        this.frameAncestors = frameAncestors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!response.containsHeader("Content-Security-Policy")) {
            response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        }
        if (!response.containsHeader("X-Frame-Options")) {
            if ("none".equalsIgnoreCase(frameAncestors)) {
                response.setHeader("X-Frame-Options", "DENY");
            } else {
                response.setHeader("X-Frame-Options", "ALLOW-FROM " + frameAncestors);
            }
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        filterChain.doFilter(request, response);
    }
}
