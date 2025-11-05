package saas.personal_branding.api.domain.model;

public enum OnboardingStatus {
    NOT_STARTED,
    PROFILE_PENDING,
    COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
