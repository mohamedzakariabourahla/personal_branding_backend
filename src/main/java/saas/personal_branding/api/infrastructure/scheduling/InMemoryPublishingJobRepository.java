package saas.personal_branding.api.infrastructure.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.domain.scheduling.PublishingJob;
import saas.personal_branding.api.domain.scheduling.PublishingJobRepository;

@Repository
@Profile("test")
public class InMemoryPublishingJobRepository implements PublishingJobRepository {

    private final ConcurrentHashMap<Long, PublishingJob> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public PublishingJob save(PublishingJob job) {
        long id = idGenerator.incrementAndGet();
        PublishingJob persisted = job.toBuilder().id(id).build();
        store.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<PublishingJob> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PublishingJob> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PublishingJob> jobs = new ArrayList<>();
        for (Long id : ids) {
            PublishingJob job = store.get(id);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    @Override
    public PublishingJob update(PublishingJob job) {
        if (job.getId() == null) {
            throw new IllegalArgumentException("Job id is required to update");
        }
        store.put(job.getId(), job);
        return job;
    }

    @Override
    public List<PublishingJob> findDue(Instant now, int max) {
        List<PublishingJob> jobs = new ArrayList<>();
        for (PublishingJob job : store.values()) {
            if (job.getScheduledAt() != null && !job.getScheduledAt().isAfter(now)) {
                jobs.add(job);
                if (jobs.size() >= max) {
                    break;
                }
            }
        }
        return jobs;
    }

    @Override
    public List<PublishingJob> findRecentByUserId(Long userId, int limit) {
        int resolvedLimit = Math.max(1, limit);
        return store.values().stream()
                .filter(job -> job.getUserId() != null && job.getUserId().equals(userId))
                .sorted(java.util.Comparator.comparing(PublishingJob::getCreatedAt).reversed())
                .limit(resolvedLimit)
                .toList();
    }
}
