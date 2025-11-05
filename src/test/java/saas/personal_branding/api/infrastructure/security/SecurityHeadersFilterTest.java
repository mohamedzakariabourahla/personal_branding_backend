package saas.personal_branding.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class SecurityHeadersFilterTest {

    @Test
    void setsDefaultHeadersWhenMissing() throws ServletException, IOException {
        SecurityHeadersFilter filter = new SecurityHeadersFilter(
                "default-src 'self'; connect-src 'self'",
                "none"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Content-Security-Policy")).contains("default-src");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("0");
        assertThat(response.getHeader("Permissions-Policy")).contains("geolocation=()");
    }

    @Test
    void setsSameOriginWhenFrameAncestorsSelf() throws ServletException, IOException {
        SecurityHeadersFilter filter = new SecurityHeadersFilter(
                "default-src 'self'",
                "'self'"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }
}
