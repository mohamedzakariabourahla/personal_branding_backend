package saas.personal_branding.api.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.application.service.EmailVerificationRateLimiter;

import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RedisEmailVerificationRateLimiter implements EmailVerificationRateLimiter {

    private static final String KEY_PREFIX = "security:email-verification:resend:";

    private static final Logger log = LoggerFactory.getLogger(RedisEmailVerificationRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration window;
    private final Counter blockedCounter;
    private final Counter sentCounter;

    public RedisEmailVerificationRateLimiter(StringRedisTemplate redisTemplate,
                                             @Value("${security.email-verification.resend-window:PT1M}") Duration window,
                                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.window = Objects.requireNonNullElse(window, Duration.ofMinutes(1));
        this.blockedCounter = meterRegistry.counter("security.email.verification.resend.blocked");
        this.sentCounter = meterRegistry.counter("security.email.verification.resend.sent");
    }

    @Override
    public void checkAllowed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = redisKey(key);
        try {
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                Long ttl = redisTemplate.getExpire(redisKey);
                long retryAfter = ttl != null && ttl > 0 ? ttl : window.getSeconds();
                blockedCounter.increment();
                throw new UserException.EmailVerificationResendRateLimitedException(retryAfter);
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while checking email verification rate limit", ex);
            throw new UserException.EmailVerificationServiceUnavailableException();
        }
    }

    @Override
    public void recordAttempt(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(redisKey(key), "1", window);
            sentCounter.increment();
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while recording email verification attempt", ex);
            throw new UserException.EmailVerificationServiceUnavailableException();
        }
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }
}
