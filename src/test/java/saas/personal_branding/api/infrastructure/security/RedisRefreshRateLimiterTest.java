package saas.personal_branding.api.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import saas.personal_branding.api.application.exception.TokenException;

@ExtendWith(MockitoExtension.class)
class RedisRefreshRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
    private final Map<String, StoredValue> store = new ConcurrentHashMap<>();
    private RedisRefreshRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            StoredValue stored = store.get(key);
            if (stored == null) {
                return null;
            }
            if (stored.expiresAt.isBefore(clock.instant())) {
                store.remove(key);
                return null;
            }
            return stored.value;
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            Duration ttl = invocation.getArgument(2);
            store.put(key, new StoredValue(value, clock.instant().plus(ttl)));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        lenient().when(redisTemplate.delete(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            store.remove(key);
            return Boolean.TRUE;
        });

        limiter = new RedisRefreshRateLimiter(redisTemplate, clock, 3, Duration.ofMinutes(10), new SimpleMeterRegistry());
    }

    @Test
    void blocksAfterExceedingMaxAttempts() {
        String token = "refresh-token";

        limiter.recordFailure(token);
        assertDoesNotThrow(() -> limiter.checkAllowed(token));

        limiter.recordFailure(token);
        assertDoesNotThrow(() -> limiter.checkAllowed(token));

        limiter.recordFailure(token);
        assertThrows(TokenException.RefreshTokenRateLimitedException.class, () -> limiter.checkAllowed(token));
    }

    @Test
    void successClearsAttempts() {
        String token = "refresh-token";
        limiter.recordFailure(token);
        limiter.recordSuccess(token);
        assertDoesNotThrow(() -> limiter.checkAllowed(token));
    }

    private record StoredValue(String value, Instant expiresAt) {
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }
    }
}
