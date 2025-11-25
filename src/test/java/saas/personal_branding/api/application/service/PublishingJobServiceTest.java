package saas.personal_branding.api.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.domain.PublishingAttemptRepository;
import saas.personal_branding.api.publishing.domain.PublishingAttemptStatus;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.domain.PublishingJobStatus;
import saas.personal_branding.api.publishing.infrastructure.scheduling.InMemoryPublishingJobQueue;
import saas.personal_branding.api.publishing.infrastructure.scheduling.InMemoryPublishingJobRepository;

class PublishingJobServiceTest {

    private PublishingJobService service;
    private InMemoryPublishingJobRepository repository;
    private PublishingAttemptRepository attemptRepository;
    private InMemoryPublishingJobQueue queue;
    private Clock clock;

    @BeforeEach
    void setup() {
        repository = new InMemoryPublishingJobRepository();
        queue = new InMemoryPublishingJobQueue();
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        attemptRepository = new NoOpPublishingAttemptRepository();
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

    private static class NoOpPublishingAttemptRepository implements PublishingAttemptRepository {
        @Override
        public PublishingAttempt save(PublishingAttempt attempt) {
            return attempt;
        }

        @Override
        public java.util.List<PublishingAttempt> findByJobId(Long jobId, int limit) {
            return java.util.List.of();
        }
    }
}
