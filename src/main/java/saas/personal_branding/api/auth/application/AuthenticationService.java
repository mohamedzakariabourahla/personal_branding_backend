package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class AuthenticationService {

    private final AuthService authService;

    public AuthenticationService(AuthService authService) {
        this.authService = authService;
    }

    public AuthService.AuthResult authenticate(AuthService.LoginCommand command, AuthService.DeviceMetadata metadata) {
        return authService.authenticate(command, metadata);
    }

    public AuthService.AuthResult refreshTokens(AuthService.RefreshTokenCommand command, AuthService.DeviceMetadata metadata) {
        return authService.refreshTokens(command, metadata);
    }

    public void logout(String refreshToken) {
        authService.logout(refreshToken);
    }

    public java.util.List<AuthService.DeviceSession> listSessions(Long userId) {
        return authService.listSessions(userId);
    }

    public void revokeSession(Long userId, String deviceId) {
        authService.revokeSession(userId, deviceId);
    }
}
