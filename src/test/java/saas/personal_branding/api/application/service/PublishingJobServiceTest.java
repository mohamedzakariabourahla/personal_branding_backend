package saas.personal_branding.api.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import saas.personal_branding.api.domain.scheduling.PublishingAttemptRepository;
import saas.personal_branding.api.domain.scheduling.PublishingJob;
import saas.personal_branding.api.domain.scheduling.PublishingJobStatus;
import saas.personal_branding.api.infrastructure.scheduling.InMemoryPublishingJobQueue;
import saas.personal_branding.api.infrastructure.scheduling.InMemoryPublishingJobRepository;

class PublishingJobServiceTest {

    private PublishingJobService service;
    private InMemoryPublishingJobRepository repository;
    PublishingAttemptRepository attemptRepository;
    private InMemoryPublishingJobQueue queue;
    private Clock clock;

    @BeforeEach
    void setup() {
        repository = new InMemoryPublishingJobRepository();
        queue = new InMemoryPublishingJobQueue();
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new PublishingJobService(repository, attemptRepository, queue, clock);
    }

    @Test
    void scheduleAndClaimAndComplete() {
        PublishingJob scheduled = service.schedule(new PublishingJobService.ScheduleCommand(
                1L, 2L, 3L, List.of("asset1"), "caption", clock.instant()
        ));

        assertThat(scheduled.getStatus()).isEqualTo(PublishingJobStatus.SCHEDULED);

        List<PublishingJob> claimed = service.claimDue(5);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getStatus()).isEqualTo(PublishingJobStatus.IN_PROGRESS);

        service.markCompleted(claimed.get(0).getId());

        PublishingJob completed = repository.findById(claimed.get(0).getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(PublishingJobStatus.SUCCEEDED);
        assertThat(completed.getCompletedAt()).isEqualTo(clock.instant());
    }
}
