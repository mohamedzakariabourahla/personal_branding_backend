package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class LogoutUseCase {

    private final AuthenticationService authenticationService;

    public LogoutUseCase(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void execute(String refreshToken) {
        authenticationService.logout(refreshToken);
    }
}
