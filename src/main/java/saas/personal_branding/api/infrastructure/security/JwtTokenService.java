package saas.personal_branding.api.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.TokenService;
import saas.personal_branding.api.application.service.dto.TokenClaims;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenService implements TokenService {

    private final Clock clock;
    private final SecretKey secretKey;
    private final Duration accessTokenTtl;
    private final String issuer;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be provided");
        }
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("security.jwt.issuer must be provided");
        }
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(resolveKeyBytes(properties.secret()));
        this.accessTokenTtl = properties.accessTokenTtl() != null ? properties.accessTokenTtl() : Duration.ofMinutes(15);
        this.issuer = properties.issuer();
    }

    private byte[] resolveKeyBytes(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to initialize JWT secret key", e);
        }
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        Instant expiry = now.plus(accessTokenTtl);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .signWith(secretKey)
                .compact();
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);

        @SuppressWarnings("unchecked")
        java.util.List<String> roleNames = claims.get("roles", java.util.List.class);
        Set<Role> roles = roleNames == null ? Set.of() : roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        return new TokenClaims(userId, email, roles);
    }
}
