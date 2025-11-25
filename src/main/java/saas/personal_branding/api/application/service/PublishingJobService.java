package saas.personal_branding.api.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.domain.PublishingJobQueue;
import saas.personal_branding.api.publishing.domain.PublishingJobRepository;
import saas.personal_branding.api.publishing.domain.PublishingJobStatus;
import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.domain.PublishingAttemptRepository;
import saas.personal_branding.api.publishing.domain.PublishingAttemptStatus;

public class PublishingJobService {

    private final PublishingJobRepository repository;
    private final PublishingAttemptRepository attemptRepository;
    private final PublishingJobQueue queue;
    private final Clock clock;

    public PublishingJobService(PublishingJobRepository repository,
                                PublishingAttemptRepository attemptRepository,
                                PublishingJobQueue queue,
                                Clock clock) {
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.queue = queue;
        this.clock = clock;
    }

    public PublishingJob schedule(ScheduleCommand command) {
        Instant now = clock.instant();
        Instant scheduledAt = Objects.requireNonNullElse(command.scheduledAt(), now);
        if (command.mediaAssetIds() == null || command.mediaAssetIds().isEmpty()) {
            throw new IllegalArgumentException("At least one media asset is required to schedule publishing.");
        }

        PublishingJob job = PublishingJob.builder()
                .id(null)
                .userId(command.userId())
                .platformId(command.platformId())
                .connectionId(command.connectionId())
                .mediaAssetIds(command.mediaAssetIds())
                .caption(command.caption())
                .scheduledAt(scheduledAt)
                .createdAt(now)
                .lastTriedAt(null)
                .completedAt(null)
                .attemptCount(0)
                .status(PublishingJobStatus.SCHEDULED)
                .failureReason(null)
                .externalPostId(null)
                .build();

        PublishingJob saved = repository.save(job);
        queue.enqueue(saved.getId(), saved.getScheduledAt());
        return saved;
    }

    public List<PublishingJob> claimDue(int maxJobs) {
        int limit = Math.max(1, maxJobs);
        List<Long> dueIds = queue.popDue(clock.instant(), limit);
        if (dueIds.isEmpty()) {
            return List.of();
        }

        List<PublishingJob> jobs = new ArrayList<>(repository.findByIds(dueIds));
        Instant now = clock.instant();
        List<PublishingJob> updated = jobs.stream()
                .map(job -> job.toBuilder()
                        .status(PublishingJobStatus.IN_PROGRESS)
                        .lastTriedAt(now)
                        .attemptCount(job.getAttemptCount() + 1)
                        .failureReason(null)
                        .build())
                .map(repository::update)
                .toList();

        return updated;
    }

    public void markCompleted(Long jobId) {
        updateStatus(jobId, PublishingJobStatus.SUCCEEDED, null, null);
    }

    public void markFailed(Long jobId, String reason, Duration retryIn) {
        if (retryIn != null && !retryIn.isNegative()) {
            Instant nextRun = clock.instant().plus(retryIn);
            updateStatus(jobId, PublishingJobStatus.SCHEDULED, reason, nextRun);
            queue.reschedule(jobId, nextRun);
        } else {
            updateStatus(jobId, PublishingJobStatus.FAILED, reason, null);
        }
    }

    public void markDeadLetter(Long jobId, String reason) {
        updateStatus(jobId, PublishingJobStatus.DEAD_LETTER, reason, null);
    }

    public Optional<PublishingJob> find(Long jobId) {
        return repository.findById(jobId);
    }

    public List<PublishingJob> findRecent(Long userId, int limit) {
        return repository.findRecentByUserId(userId, limit);
    }

    public void recordAttempt(Long jobId, PublishingAttemptStatus status, String error, String providerResponse) {
        PublishingAttempt attempt = PublishingAttempt.builder()
                .id(null)
                .jobId(jobId)
                .attemptedAt(clock.instant())
                .status(status)
                .error(error)
                .providerResponse(providerResponse)
                .build();
        attemptRepository.save(attempt);
    }

    public List<PublishingAttempt> findAttempts(Long jobId, int limit) {
        return attemptRepository.findByJobId(jobId, limit);
    }

    public void retryNow(Long jobId) {
        repository.findById(jobId).ifPresent(existing -> {
            Instant now = clock.instant();
            PublishingJob updated = existing.toBuilder()
                    .status(PublishingJobStatus.SCHEDULED)
                    .failureReason(null)
                    .scheduledAt(now)
                    .build();
            repository.update(updated);
            queue.enqueue(jobId, now);
        });
    }

    private void updateStatus(Long jobId, PublishingJobStatus status, String reason, Instant scheduledAt) {
        repository.findById(jobId).ifPresent(existing -> {
            PublishingJob updated = existing.toBuilder()
                    .status(status)
                    .failureReason(reason)
                    .externalPostId(existing.getExternalPostId())
                    .completedAt(status == PublishingJobStatus.SUCCEEDED ? clock.instant() : existing.getCompletedAt())
                    .scheduledAt(scheduledAt != null ? scheduledAt : existing.getScheduledAt())
                    .build();
            repository.update(updated);
        });
    }

    public void setExternalPostId(Long jobId, String externalPostId) {
        repository.findById(jobId).ifPresent(existing -> {
            PublishingJob updated = existing.toBuilder()
                    .externalPostId(externalPostId)
                    .build();
            repository.update(updated);
        });
    }

    public void cancel(Long jobId, String reason) {
        repository.findById(jobId).ifPresent(existing -> {
            queue.remove(jobId);
            PublishingJob updated = existing.toBuilder()
                    .status(PublishingJobStatus.DEAD_LETTER)
                    .failureReason(reason)
                    .build();
            repository.update(updated);
        });
    }

    public record ScheduleCommand(Long userId,
                                  Long platformId,
                                  Long connectionId,
                                  List<String> mediaAssetIds,
                                  String caption,
                                  Instant scheduledAt) {
    }
}
