package saas.personal_branding.api.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.PasswordResetToken;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.PasswordResetTokenRepository;
import saas.personal_branding.api.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashService tokenHashService;
    private final Clock clock;
    private final Duration tokenTtl;
    private final PasswordResetNotifier notifier;
    private final String resetBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                TokenHashService tokenHashService,
                                Clock clock,
                                Duration tokenTtl,
                                PasswordResetNotifier notifier,
                                String resetBaseUrl) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashService = tokenHashService;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.notifier = notifier;
        this.resetBaseUrl = resetBaseUrl;
    }

    public RequestResetResult requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return RequestResetResult.notIssued();
        }

        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = tokenHashService.hash(rawToken);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenTtl);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(token);

        String resetLink = resetBaseUrl + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        notifier.sendPasswordResetEmail(user.getEmail(), resetLink);

        return RequestResetResult.issued(expiresAt);
    }

    public void resetPassword(String token, String newPassword) {
        String tokenHash = tokenHashService.hash(token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(TokenException.PasswordResetTokenNotFoundException::new);

        if (resetToken.isUsed()) {
            throw new TokenException.PasswordResetTokenNotFoundException();
        }

        Instant now = clock.instant();
        if (resetToken.isExpired(now)) {
            passwordResetTokenRepository.markUsed(resetToken.getId());
            throw new TokenException.PasswordResetTokenExpiredException();
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserException.UserNotFoundException(resetToken.getUserId()));

        String passwordHash = passwordEncoder.encode(newPassword);
        User updated = user.toBuilder().passwordHash(passwordHash).build();
        userRepository.save(updated);
        passwordResetTokenRepository.markUsed(resetToken.getId());
        passwordResetTokenRepository.deleteByUserId(user.getId());
    }

    public record RequestResetResult(boolean issued, Instant expiresAt) {
        public static RequestResetResult issued(Instant expiresAt) {
            return new RequestResetResult(true, expiresAt);
        }

        public static RequestResetResult notIssued() {
            return new RequestResetResult(false, null);
        }
    }
}
