package saas.personal_branding.api.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.presentation.dto.request.LoginRequest;
import saas.personal_branding.api.presentation.dto.request.RefreshTokenRequest;
import saas.personal_branding.api.presentation.dto.request.RegisterRequest;
import saas.personal_branding.api.presentation.dto.response.AuthResponse;
import saas.personal_branding.api.presentation.mapper.UserDtoMapper;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
