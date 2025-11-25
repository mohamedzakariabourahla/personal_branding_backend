package saas.personal_branding.api.user.presentation.dto.response;

import java.util.Set;
import saas.personal_branding.api.domain.model.OnboardingStatus;

public record UserResponse(Long id,
                           String email,
                           boolean active,
                           boolean emailVerified,
                           OnboardingStatus onboardingStatus,
                           Set<String> roles,
                           PersonResponse person) {
}
