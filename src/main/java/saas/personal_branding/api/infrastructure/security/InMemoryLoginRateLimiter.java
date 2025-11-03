package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.application.service.LoginRateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final Counter failedCounter;
    private final Counter blockedCounter;
    private final Counter successCounter;

    public InMemoryLoginRateLimiter(Clock clock,
                                    @Value("${security.login.max-attempts:5}") int maxAttempts,
                                    @Value("${security.login.window:PT15M}") Duration window,
                                    MeterRegistry meterRegistry) {
        this.clock = clock;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(15));
        this.failedCounter = meterRegistry.counter("security.login.failed");
        this.blockedCounter = meterRegistry.counter("security.login.blocked");
        this.successCounter = meterRegistry.counter("security.login.success");
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
            long retryAfter = attempt.retryAfterSeconds(now, window);
            blockedCounter.increment();
            throw new UserException.TooManyLoginAttemptsException(retryAfter);
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

        private long retryAfterSeconds(Instant now, Duration window) {
            Instant windowEnd = firstFailure.plus(window);
            long seconds = Duration.between(now, windowEnd).getSeconds();
            return Math.max(seconds, 0);
        }
    }
}
