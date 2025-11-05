package saas.personal_branding.api.application.service;

import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.EmailVerificationTokenRepository;
import saas.personal_branding.api.domain.repository.UserRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final TokenHashService tokenHashService;
    private final EmailVerificationNotifier emailVerificationNotifier;
    private final Clock clock;
    private final Duration tokenTtl;
    private final String verificationBaseUrl;
    private final SecurityAuditLogger auditLogger;

    public EmailVerificationService(UserRepository userRepository,
                                    EmailVerificationTokenRepository tokenRepository,
                                    TokenHashService tokenHashService,
                                    EmailVerificationNotifier emailVerificationNotifier,
                                    Clock clock,
                                    String verificationBaseUrl,
                                    Duration tokenTtl,
                                    SecurityAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenHashService = tokenHashService;
        this.emailVerificationNotifier = emailVerificationNotifier;
        this.clock = clock;
        this.verificationBaseUrl = verificationBaseUrl;
        this.tokenTtl = tokenTtl;
        this.auditLogger = auditLogger;
    }

    public Instant sendVerificationEmail(Long userId, String email) {
        Instant expiresAt = issueToken(userId, email);
        auditLogger.log("EMAIL_VERIFICATION_SENT", Map.of(
                "userId", String.valueOf(userId),
                "email", email,
                "expiresAt", expiresAt.toString()
        ));
        return expiresAt;
    }

    public Instant resendVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.UserNotFoundException(userId));

        if (user.isEmailVerified()) {
            throw new UserException.EmailAlreadyVerifiedException(userId);
        }

        Instant expiresAt = issueToken(user.getId(), user.getEmail());
        auditLogger.log("EMAIL_VERIFICATION_RESENT", Map.of(
                "userId", String.valueOf(user.getId()),
                "email", user.getEmail(),
                "expiresAt", expiresAt.toString()
        ));
        return expiresAt;
    }

    public Optional<Instant> resendVerificationForEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .map(user -> issueToken(user.getId(), user.getEmail()));
    }

    public void verifyToken(String rawToken) {
        String tokenHash = tokenHashService.hash(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(TokenException.EmailVerificationTokenNotFoundException::new);

        if (token.isUsed()) {
            auditLogger.log("EMAIL_VERIFICATION_REJECTED", Map.of("reason", "TOKEN_USED"));
            throw new TokenException.EmailVerificationTokenNotFoundException();
        }

        Instant now = clock.instant();
        if (token.isExpired(now)) {
            tokenRepository.markUsed(token.getId());
            auditLogger.log("EMAIL_VERIFICATION_REJECTED", Map.of("reason", "TOKEN_EXPIRED"));
            throw new TokenException.EmailVerificationTokenExpiredException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserException.UserNotFoundException(token.getUserId()));

        User updated = user.toBuilder()
                .emailVerified(true)
                .emailVerifiedAt(now)
                .build();
        userRepository.save(updated);
        tokenRepository.markUsed(token.getId());
        tokenRepository.deleteByUserId(user.getId());
        auditLogger.log("EMAIL_VERIFICATION_CONFIRMED", Map.of(
                "userId", String.valueOf(user.getId()),
                "email", user.getEmail()
        ));
    }

    private Instant issueToken(Long userId, String email) {
        tokenRepository.deleteByUserId(userId);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = tokenHashService.hash(rawToken);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenTtl);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(token);

        String link = verificationBaseUrl + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailVerificationNotifier.sendEmailVerificationEmail(email, link, rawToken);
        return expiresAt;
    }
}
