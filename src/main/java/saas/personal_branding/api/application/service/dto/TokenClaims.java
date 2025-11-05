package saas.personal_branding.api.application.service.dto;

import saas.personal_branding.api.domain.model.Role;

import java.util.Set;

public record TokenClaims(Long userId, String email, Set<Role> roles) {
}
