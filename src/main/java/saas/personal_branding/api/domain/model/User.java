package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class User {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final boolean emailVerified;
    private final Instant emailVerifiedAt;

    @Singular
    private final Set<Role> roles;

    private final OnboardingStatus onboardingStatus;
    private final Person person;

    public Set<Role> getRoles() {
        return roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(Role role) {
        return getRoles().contains(role);
    }

    public boolean isOnboardingCompleted() {
        return onboardingStatus != null && onboardingStatus.isCompleted();
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
