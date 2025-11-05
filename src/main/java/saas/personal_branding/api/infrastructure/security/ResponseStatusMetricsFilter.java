package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ResponseStatusMetricsFilter extends OncePerRequestFilter {

    private final Counter clientErrorCounter;

    public ResponseStatusMetricsFilter(MeterRegistry meterRegistry) {
        this.clientErrorCounter = meterRegistry.counter("http.responses.client_error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        int status = response.getStatus();
        if (status >= 400 && status < 500) {
            clientErrorCounter.increment();
        }
    }
}
