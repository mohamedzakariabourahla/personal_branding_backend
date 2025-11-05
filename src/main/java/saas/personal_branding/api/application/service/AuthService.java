package saas.personal_branding.api.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.RefreshTokenRepository;
import saas.personal_branding.api.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final Clock clock;
    private final Duration refreshTokenTtl;
    private final LoginRateLimiter loginRateLimiter;
    private final RefreshRateLimiter refreshRateLimiter;
    private final EmailVerificationService emailVerificationService;
    private final io.micrometer.core.instrument.Counter refreshTokenRevocationCounter;
    private final io.micrometer.core.instrument.Counter refreshTokenExpiredCounter;
    private final SecurityAuditLogger auditLogger;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       TokenHashService tokenHashService,
                       Clock clock,
                       Duration refreshTokenTtl,
                       LoginRateLimiter loginRateLimiter,
                       RefreshRateLimiter refreshRateLimiter,
                       EmailVerificationService emailVerificationService,
                       io.micrometer.core.instrument.MeterRegistry meterRegistry,
                       SecurityAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
        this.loginRateLimiter = loginRateLimiter;
        this.refreshRateLimiter = refreshRateLimiter;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenRevocationCounter = meterRegistry.counter("security.refresh.revoked");
        this.refreshTokenExpiredCounter = meterRegistry.counter("security.refresh.expired");
        this.auditLogger = auditLogger;
    }

    public RegistrationResult register(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserException.EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.builder()
                .email(command.email())
                .passwordHash(passwordHash)
                .active(true)
                .emailVerified(false)
                .onboardingStatus(OnboardingStatus.NOT_STARTED)
                .role(Role.CLIENT)
                .build();

        User saved = userRepository.save(user);
        auditLogger.log("USER_REGISTERED", Map.of(
                "userId", String.valueOf(saved.getId()),
                "email", saved.getEmail()
        ));
        Instant expiresAt = emailVerificationService.sendVerificationEmail(saved.getId(), saved.getEmail());
        return new RegistrationResult(saved.getId(), saved.getEmail(), expiresAt);
    }

    @Transactional(readOnly = true)
    public User authenticateUser(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(UserException.InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new UserException.InactiveAccountException(user.getId());
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new UserException.InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            throw new UserException.EmailNotVerifiedException(user.getId());
        }

        return user;
    }

    public AuthResult authenticate(LoginCommand command) {
        loginRateLimiter.checkAllowed(command.email());
        try {
            User user = authenticateUser(command);
            AuthResult result = issueTokensFor(user);
            loginRateLimiter.recordSuccess(command.email());
            auditLogger.log("USER_LOGIN_SUCCESS", Map.of(
                    "userId", String.valueOf(user.getId()),
                    "email", user.getEmail()
            ));
            return result;
        } catch (RuntimeException ex) {
            loginRateLimiter.recordFailure(command.email());
            auditLogger.log("USER_LOGIN_FAILURE", Map.of("email", command.email()));
            throw ex;
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenHash = tokenHashService.hash(refreshToken);
        refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .ifPresent(token -> {
                    refreshTokenRepository.revokeById(token.getId());
                    refreshTokenRevocationCounter.increment();
                    auditLogger.log("USER_LOGOUT", Map.of(
                            "userId", String.valueOf(token.getUserId()),
                            "refreshTokenId", String.valueOf(token.getId())
                    ));
                });
    }

    public AuthResult refreshTokens(RefreshTokenCommand command) {
        refreshRateLimiter.checkAllowed(command.refreshToken());
        String tokenHash = tokenHashService.hash(command.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                            "reason", "NOT_FOUND"
                    ));
                    return new TokenException.RefreshTokenNotFoundException();
                });

        if (refreshToken.isExpired(clock.instant())) {
            refreshTokenRepository.revokeById(refreshToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshTokenExpiredCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                    "reason", "EXPIRED",
                    "refreshTokenId", String.valueOf(refreshToken.getId())
            ));
            throw new TokenException.RefreshTokenExpiredException();
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                            "reason", "USER_NOT_FOUND",
                            "userId", String.valueOf(refreshToken.getUserId())
                    ));
                    return new UserException.UserNotFoundException(refreshToken.getUserId());
                });

        if (!user.isActive()) {
            refreshTokenRepository.revokeById(refreshToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                    "reason", "USER_INACTIVE",
                    "userId", String.valueOf(user.getId())
            ));
            throw new UserException.InactiveAccountException(user.getId());
        }

        refreshTokenRepository.revokeById(refreshToken.getId());
        refreshTokenRevocationCounter.increment();
        AuthResult result = issueTokensFor(user);
        refreshRateLimiter.recordSuccess(command.refreshToken());
        auditLogger.log("TOKEN_REFRESH_SUCCESS", Map.of(
                "userId", String.valueOf(user.getId()),
                "refreshTokenExpiresAt", result.refreshTokenExpiresAt().toString()
        ));
        return result;
    }

    private AuthResult issueTokensFor(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        refreshTokenRevocationCounter.increment();
        String accessToken = tokenService.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = tokenHashService.hash(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.save(createRefreshToken(user.getId(), refreshTokenHash));

        return new AuthResult(user, accessToken, rawRefreshToken, refreshToken.getExpiresAt());
    }

    private RefreshToken createRefreshToken(Long userId, String tokenHash) {
        Instant now = clock.instant();
        Instant expiry = now.plus(refreshTokenTtl);
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiry)
                .createdAt(now)
                .revoked(false)
                .build();
    }

    public record RegisterUserCommand(String email, String password) {
    }

    public record LoginCommand(String email, String password) {
    }

    public record RefreshTokenCommand(String refreshToken) {
    }

    public record AuthResult(User user, String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
    }

    public record RegistrationResult(Long userId, String email, Instant verificationExpiresAt) {
    }
}
