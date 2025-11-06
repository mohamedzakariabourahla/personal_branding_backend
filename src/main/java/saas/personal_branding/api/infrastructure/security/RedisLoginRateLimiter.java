package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.application.service.LoginRateLimiter;

@Component
@ConditionalOnProperty(value = "security.login.rate-limiter", havingValue = "redis")
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginRateLimiter.class);
    private static final String KEY_PREFIX = "security:login:";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final Counter failedCounter;
    private final Counter blockedCounter;
    private final Counter successCounter;

    public RedisLoginRateLimiter(StringRedisTemplate redisTemplate,
                                 Clock clock,
                                 @Value("${security.login.max-attempts:5}") int maxAttempts,
                                 @Value("${security.login.window:PT15M}") Duration window,
                                 MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
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
        try {
            AttemptData attempt = readAttempt(key);
            if (attempt == null) {
                return;
            }

            Instant now = clock.instant();
            if (attempt.firstFailure().plus(window).isBefore(now)) {
                redisTemplate.delete(redisKey(key));
                return;
            }

            if (attempt.count() >= maxAttempts) {
                blockedCounter.increment();
                long retryAfter = Duration.between(now, attempt.firstFailure().plus(window)).toSeconds();
                throw new UserException.TooManyLoginAttemptsException(Math.max(retryAfter, 0));
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while checking login rate limit", ex);
        }
    }

    @Override
    public void recordSuccess(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(redisKey(key));
            successCounter.increment();
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while recording login success", ex);
        }
    }

    @Override
    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        failedCounter.increment();
        try {
            String redisKey = redisKey(key);
            ValueOperations<String, String> values = redisTemplate.opsForValue();
            Instant now = clock.instant();

            AttemptData attempt = readAttempt(key);
            if (attempt == null || attempt.firstFailure().plus(window).isBefore(now)) {
                AttemptData fresh = new AttemptData(1, now);
                values.set(redisKey, encode(fresh), window);
                return;
            }

            AttemptData updated = attempt.increment();
            Duration remaining = Duration.between(now, attempt.firstFailure().plus(window));
            if (remaining.isNegative() || remaining.isZero()) {
                remaining = window;
            }
            values.set(redisKey, encode(updated), remaining);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while recording login failure", ex);
        }
    }

    private AttemptData readAttempt(String key) {
        try {
            String redisKey = redisKey(key);
            String payload = redisTemplate.opsForValue().get(redisKey);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return decode(payload);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while reading login attempt", ex);
            return null;
        }
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }

    private String encode(AttemptData data) {
        return data.count() + ":" + data.firstFailure().toEpochMilli();
    }

    private AttemptData decode(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        try {
            int count = Integer.parseInt(value.substring(0, separator));
            long epochMillis = Long.parseLong(value.substring(separator + 1));
            Instant firstFailure = Instant.ofEpochMilli(epochMillis);
            return new AttemptData(Math.max(count, 0), firstFailure);
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse login rate limiter payload: {}", value, ex);
            return null;
        }
    }

    private record AttemptData(int count, Instant firstFailure) {
        AttemptData increment() {
            return new AttemptData(count + 1, firstFailure);
        }
    }
}
