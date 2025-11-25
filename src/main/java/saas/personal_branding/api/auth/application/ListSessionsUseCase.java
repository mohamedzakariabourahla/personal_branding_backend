package saas.personal_branding.api.auth.application;

import java.util.List;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.AuthService;

@Component
public class ListSessionsUseCase {

    private final AuthenticationService authenticationService;

    public ListSessionsUseCase(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public List<AuthService.DeviceSession> execute(Long userId) {
        return authenticationService.listSessions(userId);
    }
}
