package saas.personal_branding.api.auth.presentation.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.EmailVerificationRateLimiter;
import saas.personal_branding.api.auth.application.ListSessionsUseCase;
import saas.personal_branding.api.auth.application.LoginUseCase;
import saas.personal_branding.api.auth.application.LogoutUseCase;
import saas.personal_branding.api.auth.application.RefreshTokensUseCase;
import saas.personal_branding.api.auth.application.RegisterUserUseCase;
import saas.personal_branding.api.auth.application.RevokeSessionUseCase;
import saas.personal_branding.api.auth.application.ResendVerificationForEmailUseCase;
import saas.personal_branding.api.auth.application.ResendVerificationForUserUseCase;
import saas.personal_branding.api.auth.application.RequestPasswordResetUseCase;
import saas.personal_branding.api.auth.application.ResetPasswordUseCase;
import saas.personal_branding.api.auth.application.VerifyEmailUseCase;
import saas.personal_branding.api.auth.presentation.dto.request.EmailVerificationConfirmRequest;
import saas.personal_branding.api.auth.presentation.dto.request.EmailVerificationResendRequest;
import saas.personal_branding.api.auth.presentation.dto.request.LoginRequest;
import saas.personal_branding.api.auth.presentation.dto.request.PasswordResetConfirmRequest;
import saas.personal_branding.api.auth.presentation.dto.request.PasswordResetInitiateRequest;
import saas.personal_branding.api.auth.presentation.dto.request.RegisterRequest;
import saas.personal_branding.api.auth.presentation.dto.response.AuthResponse;
import saas.personal_branding.api.auth.presentation.dto.response.RegistrationPendingResponse;
import saas.personal_branding.api.auth.presentation.dto.response.SessionResponse;
import saas.personal_branding.api.user.presentation.mapper.UserDtoMapper;
import saas.personal_branding.api.domain.util.EmailNormalizer;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokensUseCase refreshTokensUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ResendVerificationForUserUseCase resendVerificationForUserUseCase;
    private final ResendVerificationForEmailUseCase resendVerificationForEmailUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final EmailVerificationRateLimiter emailVerificationRateLimiter;
    private final long verificationResendWindowSeconds;
    private final String refreshCookieName;
    private final String refreshCookiePath;
    private final String refreshCookieDomain;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final Duration refreshCookieMaxAge;
    private final Clock clock;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokensUseCase refreshTokensUseCase,
                          LogoutUseCase logoutUseCase,
                          ListSessionsUseCase listSessionsUseCase,
                          RevokeSessionUseCase revokeSessionUseCase,
                          RequestPasswordResetUseCase requestPasswordResetUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          ResendVerificationForUserUseCase resendVerificationForUserUseCase,
                          ResendVerificationForEmailUseCase resendVerificationForEmailUseCase,
                          VerifyEmailUseCase verifyEmailUseCase,
                          AuthenticatedUserProvider authenticatedUserProvider,
                          EmailVerificationRateLimiter emailVerificationRateLimiter,
                          @Value("${security.email-verification.resend-window:PT1M}") Duration verificationResendWindow,
                          @Value("${security.refresh-cookie.name:pb_refresh}") String refreshCookieName,
                          @Value("${security.refresh-cookie.path:/}") String refreshCookiePath,
                          @Value("${security.refresh-cookie.domain:}") String refreshCookieDomain,
                          @Value("${security.refresh-cookie.secure:true}") boolean refreshCookieSecure,
                          @Value("${security.refresh-cookie.same-site:None}") String refreshCookieSameSite,
                          @Value("${security.refresh-cookie.max-age:P7D}") Duration refreshCookieMaxAge,
                          Clock clock) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokensUseCase = refreshTokensUseCase;
        this.logoutUseCase = logoutUseCase;
        this.listSessionsUseCase = listSessionsUseCase;
        this.revokeSessionUseCase = revokeSessionUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.resendVerificationForUserUseCase = resendVerificationForUserUseCase;
        this.resendVerificationForEmailUseCase = resendVerificationForEmailUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.emailVerificationRateLimiter = emailVerificationRateLimiter;
        this.verificationResendWindowSeconds = Math.max(verificationResendWindow.getSeconds(), 1);
        this.refreshCookieName = refreshCookieName;
        this.refreshCookiePath = refreshCookiePath;
        this.refreshCookieDomain = refreshCookieDomain.isBlank() ? null : refreshCookieDomain;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookieMaxAge = refreshCookieMaxAge;
        this.clock = clock;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationPendingResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.RegistrationResult result = registerUserUseCase.execute(new AuthService.RegisterUserCommand(request.getEmail(), request.getPassword()));
        RegistrationPendingResponse response = new RegistrationPendingResponse(
                result.email(),
                result.verificationExpiresAt(),
                true,
                "We've sent a verification link to your email. Please confirm your address before signing in."
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse response) {
        AuthService.DeviceMetadata metadata = new AuthService.DeviceMetadata(
                null,
                request.getDeviceName(),
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        AuthService.AuthResult result = loginUseCase.execute(
                new AuthService.LoginCommand(request.getEmail(), request.getPassword()),
                metadata);
        writeRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return ResponseEntity.ok(UserDtoMapper.toAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(httpRequest);
        if (!StringUtils.hasText(refreshToken)) {
            throw new TokenException.RefreshTokenNotFoundException();
        }
        AuthService.DeviceMetadata metadata = new AuthService.DeviceMetadata(
                null,
                null,
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        AuthService.AuthResult result = refreshTokensUseCase.execute(new AuthService.RefreshTokenCommand(refreshToken), metadata);
        writeRefreshCookie(httpResponse, result.refreshToken(), result.refreshTokenExpiresAt());
        return ResponseEntity.ok(UserDtoMapper.toAuthResponse(result));
    }

    @GetMapping("/session")
    public ResponseEntity<AuthResponse> bootstrapSession(HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(httpRequest);
        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthService.DeviceMetadata metadata = new AuthService.DeviceMetadata(
                null,
                null,
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        AuthService.AuthResult result = refreshTokensUseCase.execute(new AuthService.RefreshTokenCommand(refreshToken), metadata);
        writeRefreshCookie(httpResponse, result.refreshToken(), result.refreshTokenExpiresAt());
        return ResponseEntity.ok(UserDtoMapper.toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(httpRequest);
        if (StringUtils.hasText(refreshToken)) {
            try {
                logoutUseCase.execute(refreshToken);
            } catch (Exception ex) {
                log.warn("Failed to revoke refresh token during logout", ex);
            }
        }
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/resend")
    public ResponseEntity<Void> resendVerificationEmail() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        resendVerificationForUserUseCase.execute(userId);
        return ResponseEntity.accepted()
                .header("Retry-After", String.valueOf(verificationResendWindowSeconds))
                .build();
    }

    @PostMapping("/email/resend-guest")
    public ResponseEntity<Void> resendVerificationEmailGuest(@Valid @RequestBody EmailVerificationResendRequest request,
                                                             HttpServletRequest httpRequest) {
        String key = buildRateLimitKey(request.getEmail(), httpRequest);
        emailVerificationRateLimiter.checkAllowed(key);
        try {
            String email = request.getEmail() != null ? request.getEmail().trim() : null;
            resendVerificationForEmailUseCase.execute(email);
        } finally {
            emailVerificationRateLimiter.recordAttempt(key);
        }
        return ResponseEntity.accepted()
                .header("Retry-After", String.valueOf(verificationResendWindowSeconds))
                .build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        verifyEmailUseCase.execute(request.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetInitiateRequest request) {
        try {
            var result = requestPasswordResetUseCase.execute(request.getEmail());
            if (result.issued()) {
                log.debug("Password reset token issued for {} expiring at {}", request.getEmail(), result.expiresAt());
            }
        } catch (Exception ex) {
            log.error("Failed to issue password reset token for {}", request.getEmail(), ex);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        resetPasswordUseCase.execute(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        List<SessionResponse> sessions = listSessionsUseCase.execute(userId).stream()
                .map(session -> new SessionResponse(
                        session.deviceId(),
                        session.deviceName(),
                        session.userAgent(),
                        session.ipAddress(),
                        session.createdAt(),
                        session.lastUsedAt(),
                        session.expiresAt()))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/sessions/{deviceId}")
    public ResponseEntity<Void> revokeSession(@PathVariable String deviceId) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        revokeSessionUseCase.execute(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    private String buildRateLimitKey(String email, HttpServletRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        String clientIp = resolveClientIp(request);
        return (normalizedEmail == null ? "" : normalizedEmail) + "|" + clientIp;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && refreshCookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken, Instant expiresAt) {
        if (response == null) {
            return;
        }
        if (!StringUtils.hasText(refreshToken)) {
            clearRefreshCookie(response);
            return;
        }

        Duration maxAge = determineCookieMaxAge(expiresAt);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path(refreshCookiePath)
                .sameSite(refreshCookieSameSite)
                .maxAge(maxAge);
        if (refreshCookieDomain != null) {
            builder.domain(refreshCookieDomain);
        }

        ResponseCookie cookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path(refreshCookiePath)
                .sameSite(refreshCookieSameSite)
                .maxAge(Duration.ZERO);
        if (refreshCookieDomain != null) {
            builder.domain(refreshCookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private Duration determineCookieMaxAge(Instant expiresAt) {
        if (expiresAt == null) {
            return refreshCookieMaxAge;
        }
        Duration candidate = Duration.between(clock.instant(), expiresAt);
        if (candidate.isNegative()) {
            return Duration.ZERO;
        }
        return candidate;
    }
}
