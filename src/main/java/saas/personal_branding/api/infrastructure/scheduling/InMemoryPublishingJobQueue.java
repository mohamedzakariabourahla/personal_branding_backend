package saas.personal_branding.api.infrastructure.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.scheduling.PublishingJobQueue;

@Component
@Profile("test")
public class InMemoryPublishingJobQueue implements PublishingJobQueue {

    private static class QueueEntry implements Comparable<QueueEntry> {
        private final Long jobId;
        private final Instant scheduledAt;

        private QueueEntry(Long jobId, Instant scheduledAt) {
            this.jobId = jobId;
            this.scheduledAt = scheduledAt;
        }

        @Override
        public int compareTo(QueueEntry other) {
            return this.scheduledAt.compareTo(other.scheduledAt);
        }
    }

    private final PriorityQueue<QueueEntry> queue = new PriorityQueue<>();
    private final Map<Long, QueueEntry> index = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void enqueue(Long jobId, Instant scheduledAt) {
        lock.lock();
        try {
            QueueEntry entry = new QueueEntry(jobId, scheduledAt);
            queue.add(entry);
            index.put(jobId, entry);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Long> popDue(Instant now, int max) {
        List<Long> due = new ArrayList<>();
        lock.lock();
        try {
            while (!queue.isEmpty() && due.size() < max) {
                QueueEntry entry = queue.peek();
                if (entry.scheduledAt.isAfter(now)) {
                    break;
                }
                queue.poll();
                index.remove(entry.jobId);
                due.add(entry.jobId);
            }
        } finally {
            lock.unlock();
        }
        return due;
    }

    @Override
    public void reschedule(Long jobId, Instant scheduledAt) {
        lock.lock();
        try {
            QueueEntry existing = index.remove(jobId);
            if (existing != null) {
                queue.remove(existing);
            }
            enqueue(jobId, scheduledAt);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(Long jobId) {
        lock.lock();
        try {
            QueueEntry entry = index.remove(jobId);
            if (entry != null) {
                queue.remove(entry);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Instant peekScheduledAt() {
        lock.lock();
        try {
            QueueEntry entry = queue.peek();
            return entry != null ? entry.scheduledAt : null;
        } finally {
            lock.unlock();
        }
    }
}
