package saas.personal_branding.api.application.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.scheduling.PublishingJobQueue;

@Component
@ConditionalOnProperty(value = "app.scheduling.worker.enabled", havingValue = "true")
public class PublishingQueueMetrics {

    public PublishingQueueMetrics(PublishingJobQueue queue, MeterRegistry meterRegistry) {
        Gauge.builder("publishing.queue.depth", queue, PublishingJobQueue::size)
                .description("Number of jobs in publishing queue")
                .register(meterRegistry);

        Gauge.builder("publishing.queue.lag.seconds", queue, q -> computeLagSeconds(q.peekScheduledAt()))
                .description("Seconds until the next job is scheduled (negative if overdue)")
                .register(meterRegistry);
    }

    private double computeLagSeconds(Instant scheduledAt) {
        if (scheduledAt == null) {
            return 0;
        }
        return Duration.between(Instant.now(), scheduledAt).toSeconds();
    }
}
