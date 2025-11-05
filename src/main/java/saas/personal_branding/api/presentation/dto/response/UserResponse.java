package saas.personal_branding.api.presentation.dto.response;

import saas.personal_branding.api.domain.model.OnboardingStatus;

import java.util.Set;

public record UserResponse(Long id,
                           String email,
                           boolean active,
                           boolean emailVerified,
                           OnboardingStatus onboardingStatus,
                           Set<String> roles,
                           PersonResponse person) {
}
