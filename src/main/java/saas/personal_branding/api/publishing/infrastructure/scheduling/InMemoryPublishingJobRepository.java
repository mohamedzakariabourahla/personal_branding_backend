package saas.personal_branding.api.publishing.infrastructure.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.domain.PublishingJobRepository;

public class InMemoryPublishingJobRepository implements PublishingJobRepository {

    private final Map<Long, PublishingJob> store = new HashMap<>();
    private long sequence = 0;

    @Override
    public PublishingJob save(PublishingJob job) {
        long id = ++sequence;
        PublishingJob toStore = job.toBuilder().id(id).createdAt(Instant.now()).build();
        store.put(id, toStore);
        return toStore;
    }

    @Override
    public Optional<PublishingJob> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PublishingJob> findByIds(List<Long> ids) {
        return ids.stream()
                .map(store::get)
                .filter(j -> j != null)
                .toList();
    }

    @Override
    public PublishingJob update(PublishingJob job) {
        store.put(job.getId(), job);
        return job;
    }

    @Override
    public List<PublishingJob> findDue(Instant now, int max) {
        List<PublishingJob> due = new ArrayList<>();
        for (PublishingJob job : store.values()) {
            if (job.getScheduledAt().isBefore(now) || job.getScheduledAt().equals(now)) {
                due.add(job);
            }
            if (due.size() >= max) {
                break;
            }
        }
        return due;
    }

    @Override
    public List<PublishingJob> findRecentByUserId(Long userId, int limit) {
        return store.values().stream()
                .filter(job -> job.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }
}
