package saas.personal_branding.api.publishing.domain;

import java.time.Instant;
import java.util.List;

public interface PublishingJobQueue {

    void enqueue(Long jobId, Instant scheduledAt);

    List<Long> popDue(Instant now, int max);

    void reschedule(Long jobId, Instant scheduledAt);

    void remove(Long jobId);

    int size();

    /**
     * Returns the earliest scheduled time if available.
     */
    Instant peekScheduledAt();
}
