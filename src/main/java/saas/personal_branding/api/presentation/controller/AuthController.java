package saas.personal_branding.api.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.PasswordResetService;
import saas.personal_branding.api.application.service.EmailVerificationService;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import saas.personal_branding.api.application.service.EmailVerificationRateLimiter;
import saas.personal_branding.api.presentation.dto.request.LoginRequest;
import saas.personal_branding.api.presentation.dto.request.LogoutRequest;
import saas.personal_branding.api.presentation.dto.request.PasswordResetConfirmRequest;
import saas.personal_branding.api.presentation.dto.request.PasswordResetInitiateRequest;
import saas.personal_branding.api.presentation.dto.request.EmailVerificationConfirmRequest;
import saas.personal_branding.api.presentation.dto.request.EmailVerificationResendRequest;
import saas.personal_branding.api.presentation.dto.request.RefreshTokenRequest;
import saas.personal_branding.api.presentation.dto.request.RegisterRequest;
import saas.personal_branding.api.presentation.dto.response.AuthResponse;
import saas.personal_branding.api.presentation.dto.response.RegistrationPendingResponse;
import saas.personal_branding.api.presentation.mapper.UserDtoMapper;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final EmailVerificationRateLimiter emailVerificationRateLimiter;
    private final long verificationResendWindowSeconds;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService,
                          AuthenticatedUserProvider authenticatedUserProvider,
                          EmailVerificationRateLimiter emailVerificationRateLimiter,
                          @Value("${security.email-verification.resend-window:PT1M}") Duration verificationResendWindow) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.emailVerificationRateLimiter = emailVerificationRateLimiter;
        this.verificationResendWindowSeconds = Math.max(verificationResendWindow.getSeconds(), 1);
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationPendingResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.RegistrationResult result = authService.register(new AuthService.RegisterUserCommand(request.getEmail(), request.getPassword()));
        RegistrationPendingResponse response = new RegistrationPendingResponse(
                result.email(),
                result.verificationExpiresAt(),
                true,
                "We've sent a verification link to your email. Please confirm your address before signing in."
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.authenticate(new AuthService.LoginCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok(UserDtoMapper.toAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthService.AuthResult result = authService.refreshTokens(new AuthService.RefreshTokenCommand(request.getRefreshToken()));
        return ResponseEntity.ok(UserDtoMapper.toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            authService.logout(request.getRefreshToken());
        } catch (Exception ex) {
            log.warn("Failed to revoke refresh token during logout", ex);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/resend")
    public ResponseEntity<Void> resendVerificationEmail() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        emailVerificationService.resendVerification(userId);
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
            emailVerificationService.resendVerificationForEmail(email);
        } finally {
            emailVerificationRateLimiter.recordAttempt(key);
        }
        return ResponseEntity.accepted()
                .header("Retry-After", String.valueOf(verificationResendWindowSeconds))
                .build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.verifyToken(request.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetInitiateRequest request) {
        try {
            PasswordResetService.RequestResetResult result = passwordResetService.requestPasswordReset(request.getEmail());
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
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    private String buildRateLimitKey(String email, HttpServletRequest request) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String clientIp = resolveClientIp(request);
        return normalizedEmail + "|" + clientIp;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
