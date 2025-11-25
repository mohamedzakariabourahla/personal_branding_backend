package saas.personal_branding.api.auth.application;

import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class RefreshTokensUseCase {

    private final AuthenticationService authenticationService;

    public RefreshTokensUseCase(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public AuthService.AuthResult execute(AuthService.RefreshTokenCommand command,
                                          AuthService.DeviceMetadata metadata) {
        return authenticationService.refreshTokens(command, metadata);
    }
}
