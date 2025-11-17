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
import saas.personal_branding.api.domain.util.EmailNormalizer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private final int maxActiveSessions;

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
                       int maxActiveSessions,
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
        this.maxActiveSessions = Math.max(maxActiveSessions, 1);
    }

    public RegistrationResult register(RegisterUserCommand command) {
        String email = command.email();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserException.EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.builder()
                .email(email)
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
        String email = command.email();
        if (email == null || email.isBlank()) {
            throw new UserException.InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(email)
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

    public AuthResult authenticate(LoginCommand command, DeviceMetadata metadata) {
        loginRateLimiter.checkAllowed(command.email());
        try {
            User user = authenticateUser(command);
            AuthResult result = issueTokensFor(user, sanitizeMetadata(metadata, null));
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

    public AuthResult refreshTokens(RefreshTokenCommand command, DeviceMetadata metadata) {
        refreshRateLimiter.checkAllowed(command.refreshToken());
        String tokenHash = tokenHashService.hash(command.refreshToken());
        RefreshToken currentToken = refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                            "reason", "NOT_FOUND"
                    ));
                    return new TokenException.RefreshTokenNotFoundException();
                });

        if (currentToken.isExpired(clock.instant())) {
            refreshTokenRepository.revokeById(currentToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshTokenExpiredCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                    "reason", "EXPIRED",
                    "refreshTokenId", String.valueOf(currentToken.getId())
            ));
            throw new TokenException.RefreshTokenExpiredException();
        }

        User user = userRepository.findById(currentToken.getUserId())
                .orElseThrow(() -> {
                    refreshRateLimiter.recordFailure(command.refreshToken());
                    auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                            "reason", "USER_NOT_FOUND",
                            "userId", String.valueOf(currentToken.getUserId())
                    ));
                    return new UserException.UserNotFoundException(currentToken.getUserId());
                });

        if (!user.isActive()) {
            refreshTokenRepository.revokeById(currentToken.getId());
            refreshTokenRevocationCounter.increment();
            refreshRateLimiter.recordFailure(command.refreshToken());
            auditLogger.log("TOKEN_REFRESH_FAILED", Map.of(
                    "reason", "USER_INACTIVE",
                    "userId", String.valueOf(user.getId())
            ));
            throw new UserException.InactiveAccountException(user.getId());
        }

        refreshTokenRepository.revokeById(currentToken.getId());
        refreshTokenRevocationCounter.increment();

        AuthResult result = issueTokensFor(user, sanitizeMetadata(metadata, currentToken));
        refreshRateLimiter.recordSuccess(command.refreshToken());
        auditLogger.log("TOKEN_REFRESH_SUCCESS", Map.of(
                "userId", String.valueOf(user.getId()),
                "refreshTokenExpiresAt", result.refreshTokenExpiresAt().toString()
        ));
        return result;
    }

    private AuthResult issueTokensFor(User user, DeviceMetadata metadata) {
        DeviceMetadata preparedMetadata = ensureDeviceMetadata(metadata);

        int revoked = refreshTokenRepository.revokeByUserIdAndDeviceId(user.getId(), preparedMetadata.deviceId());
        if (revoked > 0) {
            refreshTokenRevocationCounter.increment(revoked);
        }

        String accessToken = tokenService.generateAccessToken(user);
        Instant issuedAt = clock.instant();
        Instant expiry = issuedAt.plus(refreshTokenTtl);
        String rawRefreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = tokenHashService.hash(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHash)
                .expiresAt(expiry)
                .createdAt(issuedAt)
                .revoked(false)
                .deviceId(preparedMetadata.deviceId())
                .deviceName(preparedMetadata.deviceName())
                .userAgent(preparedMetadata.userAgent())
                .ipAddress(preparedMetadata.ipAddress())
                .lastUsedAt(issuedAt)
                .build();

        RefreshToken persisted = refreshTokenRepository.save(refreshToken);
        enforceSessionLimit(user.getId(), persisted.getId());

        return new AuthResult(
                user,
                accessToken,
                rawRefreshToken,
                persisted.getExpiresAt(),
                persisted.getCreatedAt(),
                persisted.getDeviceId(),
                persisted.getDeviceName());
    }

    private void enforceSessionLimit(Long userId, Long tokenIdToKeep) {
        if (maxActiveSessions <= 0) {
            return;
        }

        var activeTokens = new ArrayList<>(refreshTokenRepository.findActiveByUserId(userId));
        int activeCount = activeTokens.size();
        if (activeCount <= maxActiveSessions) {
            return;
        }

        activeTokens.sort(java.util.Comparator.comparing(RefreshToken::getLastUsedAt));
        for (RefreshToken token : activeTokens) {
            if (activeCount <= maxActiveSessions) {
                break;
            }
            if (tokenIdToKeep != null && tokenIdToKeep.equals(token.getId())) {
                continue;
            }
            refreshTokenRepository.revokeById(token.getId());
            refreshTokenRevocationCounter.increment();
            activeCount--;
        }
    }

    private DeviceMetadata sanitizeMetadata(DeviceMetadata metadata, RefreshToken fallback) {
        String deviceId = metadata != null ? metadata.deviceId() : null;
        String deviceName = metadata != null ? metadata.deviceName() : null;
        String userAgent = metadata != null ? metadata.userAgent() : null;
        String ipAddress = metadata != null ? metadata.ipAddress() : null;

        if (deviceId == null && fallback != null) {
            deviceId = fallback.getDeviceId();
        }
        if (deviceName == null && fallback != null) {
            deviceName = fallback.getDeviceName();
        }
        if (userAgent == null && fallback != null) {
            userAgent = fallback.getUserAgent();
        }
        if (ipAddress == null && fallback != null) {
            ipAddress = fallback.getIpAddress();
        }

        return new DeviceMetadata(deviceId, deviceName, userAgent, ipAddress);
    }

    private DeviceMetadata ensureDeviceMetadata(DeviceMetadata metadata) {
        String deviceId = metadata.deviceId();
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        } else {
            deviceId = truncate(deviceId, 64);
        }

        String userAgent = truncate(metadata.userAgent(), 512);
        String ipAddress = truncate(metadata.ipAddress(), 45);
        String deviceName = resolveDeviceName(metadata.deviceName(), userAgent);

        return new DeviceMetadata(deviceId, deviceName, userAgent, ipAddress);
    }

    private String resolveDeviceName(String explicitName, String userAgent) {
        String candidate = explicitName != null && !explicitName.isBlank()
                ? explicitName
                : userAgent != null && !userAgent.isBlank() ? userAgent : "Unknown Device";
        return truncate(candidate, 120);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public java.util.List<DeviceSession> listSessions(Long userId) {
        Instant now = clock.instant();
        return refreshTokenRepository.findActiveByUserId(userId).stream()
                .filter(token -> !token.isExpired(now))
                .map(token -> new DeviceSession(
                        token.getDeviceId(),
                        token.getDeviceName(),
                        token.getUserAgent(),
                        token.getIpAddress(),
                        token.getCreatedAt(),
                        token.getLastUsedAt(),
                        token.getExpiresAt()))
                .toList();
    }

    public void revokeSession(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        int revoked = refreshTokenRepository.revokeByUserIdAndDeviceId(userId, deviceId.trim());
        if (revoked > 0) {
            refreshTokenRevocationCounter.increment(revoked);
            auditLogger.log("USER_SESSION_REVOKED", Map.of(
                    "userId", String.valueOf(userId),
                    "deviceId", deviceId.trim()
            ));
        }
    }

    public record RegisterUserCommand(String email, String password) {
        public RegisterUserCommand(String email, String password) {
            this.email = EmailNormalizer.normalize(email);
            this.password = password;
        }
    }

    public record LoginCommand(String email, String password) {
        public LoginCommand(String email, String password) {
            this.email = EmailNormalizer.normalize(email);
            this.password = password;
        }
    }

    public record RefreshTokenCommand(String refreshToken) {
    }

    public record AuthResult(User user,
                             String accessToken,
                             String refreshToken,
                             Instant refreshTokenExpiresAt,
                             Instant refreshTokenIssuedAt,
                             String deviceId,
                             String deviceName) {
    }

    public record RegistrationResult(Long userId, String email, Instant verificationExpiresAt) {
    }

    public record DeviceMetadata(String deviceId, String deviceName, String userAgent, String ipAddress) {
    }

    public record DeviceSession(String deviceId,
                                String deviceName,
                                String userAgent,
                                String ipAddress,
                                Instant createdAt,
                                Instant lastUsedAt,
                                Instant expiresAt) {
    }
}
