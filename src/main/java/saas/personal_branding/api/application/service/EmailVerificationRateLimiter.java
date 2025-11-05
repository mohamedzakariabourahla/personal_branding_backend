package saas.personal_branding.api.application.service;

public interface EmailVerificationRateLimiter {

    void checkAllowed(String key);

    void recordAttempt(String key);
}
