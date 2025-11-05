package saas.personal_branding.api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import saas.personal_branding.api.application.service.TokenService;
import saas.personal_branding.api.application.service.dto.TokenClaims;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.UserRepository;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final Counter invalidJwtCounter;

    public JwtAuthenticationFilter(TokenService tokenService,
                                   UserRepository userRepository,
                                   MeterRegistry meterRegistry) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.invalidJwtCounter = meterRegistry.counter("security.jwt.invalid");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null) {
            try {
                TokenClaims claims = tokenService.parseAccessToken(token);

                userRepository.findById(claims.userId())
                        .filter(User::isActive)
                        .ifPresent(user -> authenticate(user));
            } catch (Exception ex) {
                invalidJwtCounter.increment();
                log.warn("JWT validation failed: method={} path={} reason={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage());
                log.debug("JWT validation error details", ex);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {
        Set<SimpleGrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toUnmodifiableSet());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
        authentication.setDetails(user);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
