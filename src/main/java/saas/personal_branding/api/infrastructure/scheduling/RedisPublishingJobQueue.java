package saas.personal_branding.api.infrastructure.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.scheduling.PublishingJobQueue;

@Component
@Profile("!test")
public class RedisPublishingJobQueue implements PublishingJobQueue {

    private final StringRedisTemplate redisTemplate;
    private final String queueKey;

    public RedisPublishingJobQueue(StringRedisTemplate redisTemplate,
                                   @Value("${app.scheduling.queue.key:publishing:due}") String queueKey) {
        this.redisTemplate = redisTemplate;
        this.queueKey = queueKey;
    }

    @Override
    public void enqueue(Long jobId, Instant scheduledAt) {
        redisTemplate.opsForZSet().add(queueKey, jobId.toString(), asScore(scheduledAt));
    }

    @Override
    public List<Long> popDue(Instant now, int max) {
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        double score = asScore(now);
        List<String> ids = new ArrayList<>(zset.rangeByScore(queueKey, 0, score, 0, max));
        if (ids.isEmpty()) {
            return List.of();
        }
        zset.remove(queueKey, ids.toArray());
        return ids.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    @Override
    public void reschedule(Long jobId, Instant scheduledAt) {
        enqueue(jobId, scheduledAt);
    }

    @Override
    public void remove(Long jobId) {
        redisTemplate.opsForZSet().remove(queueKey, jobId.toString());
    }

    @Override
    public int size() {
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size == null ? 0 : size.intValue();
    }

    @Override
    public Instant peekScheduledAt() {
        var range = redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, 0);
        if (range == null || range.isEmpty()) {
            return null;
        }
        var tuple = range.stream().findFirst().orElse(null);
        if (tuple == null || tuple.getScore() == null) {
            return null;
        }
        return Instant.ofEpochMilli(tuple.getScore().longValue());
    }

    private double asScore(Instant instant) {
        return instant.toEpochMilli();
    }
}
