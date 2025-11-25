package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class RevokeSessionUseCase {

    private final AuthenticationService authenticationService;

    public RevokeSessionUseCase(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void execute(Long userId, String deviceId) {
        authenticationService.revokeSession(userId, deviceId);
    }
}
