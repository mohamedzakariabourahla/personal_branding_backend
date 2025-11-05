package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.service.RefreshRateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRefreshRateLimiter implements RefreshRateLimiter {

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final Counter failedCounter;
    private final Counter blockedCounter;
    private final Counter successCounter;

    public InMemoryRefreshRateLimiter(Clock clock,
                                      @Value("${security.refresh.max-attempts:20}") int maxAttempts,
                                      @Value("${security.refresh.window:PT10M}") Duration window,
                                      MeterRegistry meterRegistry) {
        this.clock = clock;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(10));
        this.failedCounter = meterRegistry.counter("security.refresh.failed");
        this.blockedCounter = meterRegistry.counter("security.refresh.blocked");
        this.successCounter = meterRegistry.counter("security.refresh.success");
    }

    @Override
    public void checkAllowed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }

        Instant now = clock.instant();
        if (attempt.isExpired(now, window)) {
            attempts.remove(key);
            return;
        }

        if (attempt.count >= maxAttempts) {
            blockedCounter.increment();
            long retryAfter = Duration.between(now, attempt.firstFailure.plus(window)).getSeconds();
            throw new TokenException.RefreshTokenRateLimitedException(Math.max(retryAfter, 0));
        }
    }

    @Override
    public void recordSuccess(String key) {
        if (key != null && !key.isBlank()) {
            attempts.remove(key);
            successCounter.increment();
        }
    }

    @Override
    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        Instant now = clock.instant();
        attempts.compute(key, (k, attempt) -> {
            if (attempt == null || attempt.isExpired(now, window)) {
                failedCounter.increment();
                return new Attempt(1, now);
            }

            failedCounter.increment();
            return attempt.increment();
        });
    }

    private static final class Attempt {
        private final int count;
        private final Instant firstFailure;

        private Attempt(int count, Instant firstFailure) {
            this.count = count;
            this.firstFailure = firstFailure;
        }

        private boolean isExpired(Instant now, Duration window) {
            return firstFailure.plus(window).isBefore(now);
        }

        private Attempt increment() {
            return new Attempt(count + 1, firstFailure);
        }
    }
}
