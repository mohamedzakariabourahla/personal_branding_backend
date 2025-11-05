package saas.personal_branding.api.application.service;

public interface RefreshRateLimiter {
    void checkAllowed(String key);
    void recordSuccess(String key);
    void recordFailure(String key);
}
