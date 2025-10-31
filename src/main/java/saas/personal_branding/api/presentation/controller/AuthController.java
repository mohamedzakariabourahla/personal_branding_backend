package saas.personal_branding.api.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.presentation.dto.request.LoginRequest;
import saas.personal_branding.api.presentation.dto.request.RegisterRequest;
import saas.personal_branding.api.presentation.dto.response.UserResponse;
import saas.personal_branding.api.presentation.mapper.UserDtoMapper;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.register(new AuthService.RegisterUserCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDtoMapper.toUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
        User user = authService.authenticate(new AuthService.LoginCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok(UserDtoMapper.toUserResponse(user));
    }
}
