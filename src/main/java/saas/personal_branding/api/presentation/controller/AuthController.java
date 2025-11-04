package saas.personal_branding.api.presentation.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.PasswordResetService;
import saas.personal_branding.api.presentation.dto.request.LoginRequest;
import saas.personal_branding.api.presentation.dto.request.LogoutRequest;
import saas.personal_branding.api.presentation.dto.request.PasswordResetConfirmRequest;
import saas.personal_branding.api.presentation.dto.request.PasswordResetInitiateRequest;
import saas.personal_branding.api.presentation.dto.request.RefreshTokenRequest;
import saas.personal_branding.api.presentation.dto.request.RegisterRequest;
import saas.personal_branding.api.presentation.dto.response.AuthResponse;
import saas.personal_branding.api.presentation.mapper.UserDtoMapper;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(new AuthService.RegisterUserCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDtoMapper.toAuthResponse(result));
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
}
