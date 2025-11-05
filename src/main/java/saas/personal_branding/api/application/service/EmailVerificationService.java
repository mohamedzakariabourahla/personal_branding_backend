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

    public EmailVerificationService(UserRepository userRepository,
                                    EmailVerificationTokenRepository tokenRepository,
                                    TokenHashService tokenHashService,
                                    EmailVerificationNotifier emailVerificationNotifier,
                                    Clock clock,
                                    String verificationBaseUrl,
                                    Duration tokenTtl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenHashService = tokenHashService;
        this.emailVerificationNotifier = emailVerificationNotifier;
        this.clock = clock;
        this.verificationBaseUrl = verificationBaseUrl;
        this.tokenTtl = tokenTtl;
    }

    public Instant sendVerificationEmail(Long userId, String email) {
        return issueToken(userId, email);
    }

    public Instant resendVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.UserNotFoundException(userId));

        if (user.isEmailVerified()) {
            throw new UserException.EmailAlreadyVerifiedException(userId);
        }

        return issueToken(user.getId(), user.getEmail());
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
            throw new TokenException.EmailVerificationTokenNotFoundException();
        }

        Instant now = clock.instant();
        if (token.isExpired(now)) {
            tokenRepository.markUsed(token.getId());
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
