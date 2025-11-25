package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class RegistrationService {

    private final AuthService authService;

    public RegistrationService(AuthService authService) {
        this.authService = authService;
    }

    public AuthService.RegistrationResult register(AuthService.RegisterUserCommand command) {
        return authService.register(command);
    }
}
