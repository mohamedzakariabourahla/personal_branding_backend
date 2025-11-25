package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class RegisterUserUseCase {

    private final RegistrationService registrationService;

    public RegisterUserUseCase(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    public AuthService.RegistrationResult execute(AuthService.RegisterUserCommand command) {
        return registrationService.register(command);
    }
}
