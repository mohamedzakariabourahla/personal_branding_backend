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
    private final io.micrometer.core.instrument.Counter refreshTokenRevocationCounter;
    private final io.micrometer.core.instrument.Counter refreshTokenExpiredCounter;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       TokenHashService tokenHashService,
                       Clock clock,
                       Duration refreshTokenTtl,
                       LoginRateLimiter loginRateLimiter,
                       RefreshRateLimiter refreshRateLimiter,
                       io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
        this.loginRateLimiter = loginRateLimiter;
        this.refreshRateLimiter = refreshRateLimiter;
        this.refreshTokenRevocationCounter = meterRegistry.counter("security.refresh.revoked");
        this.refreshTokenExpiredCounter = meterRegistry.counter("security.refresh.expired");
    }

    public AuthResult register(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserException.EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.builder()
                .email(command.email())
                .passwordHash(passwordHash)
                .active(true)
                .onboardingStatus(OnboardingStatus.NOT_STARTED)
                .role(Role.CLIENT)
                .build();

        User saved = userRepository.save(user);
        return issueTokensFor(saved);
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

        return user;
    }

    public AuthResult authenticate(LoginCommand command) {
        loginRateLimiter.checkAllowed(command.email());
        try {
            User user = authenticateUser(command);
            AuthResult result = issueTokensFor(user);
            loginRateLimiter.recordSuccess(command.email());
            return result;
        } catch (RuntimeException ex) {
            loginRateLimiter.recordFailure(command.email());
            throw ex;
        }
    }

    public AuthResult refreshTokens(RefreshTokenCommand command) {
        refreshRateLimiter.checkAllowed(command.refreshToken());
        String tokenHash = tokenHashService.hash(command.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    return new TokenException.RefreshTokenNotFoundException();
                });

        if (refreshToken.isExpired(clock.instant())) {
            refreshTokenRepository.revokeById(refreshToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshTokenExpiredCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            throw new TokenException.RefreshTokenExpiredException();
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    return new UserException.UserNotFoundException(refreshToken.getUserId());
                });

        if (!user.isActive()) {
            refreshTokenRepository.revokeById(refreshToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            throw new UserException.InactiveAccountException(user.getId());
        }

        refreshTokenRepository.revokeById(refreshToken.getId());
        refreshTokenRevocationCounter.increment();
        AuthResult result = issueTokensFor(user);
        refreshRateLimiter.recordSuccess(command.refreshToken());
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
}
