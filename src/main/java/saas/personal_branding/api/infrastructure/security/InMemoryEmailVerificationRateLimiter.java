package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.application.service.EmailVerificationRateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEmailVerificationRateLimiter implements EmailVerificationRateLimiter {

    private final ConcurrentHashMap<String, Instant> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration window;
    private final Counter blockedCounter;
    private final Counter sentCounter;

    public InMemoryEmailVerificationRateLimiter(Clock clock,
                                                @Value("${security.email-verification.resend-window:PT1M}") Duration window,
                                                MeterRegistry meterRegistry) {
        this.clock = clock;
        this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(1));
        this.blockedCounter = meterRegistry.counter("security.email.verification.resend.blocked");
        this.sentCounter = meterRegistry.counter("security.email.verification.resend.sent");
    }

    @Override
    public void checkAllowed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        Instant lastAttempt = attempts.get(key);
        if (lastAttempt == null) {
            return;
        }

        Instant now = clock.instant();
        Instant allowedAt = lastAttempt.plus(window);
        if (!allowedAt.isBefore(now)) {
            long retryAfter = Math.max(Duration.between(now, allowedAt).getSeconds(), 0);
            blockedCounter.increment();
            throw new UserException.EmailVerificationResendRateLimitedException(retryAfter);
        }
    }

    @Override
    public void recordAttempt(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        attempts.put(key, clock.instant());
        sentCounter.increment();
    }
}
