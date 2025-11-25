package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class LoginUseCase {

    private final AuthenticationService authenticationService;

    public LoginUseCase(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public AuthService.AuthResult execute(AuthService.LoginCommand command,
                                          AuthService.DeviceMetadata metadata) {
        return authenticationService.authenticate(command, metadata);
    }
}
