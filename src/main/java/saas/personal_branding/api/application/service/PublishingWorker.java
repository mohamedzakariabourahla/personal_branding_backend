package saas.personal_branding.api.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.platform.PlatformPublishCommand;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.PlatformPublisherRegistry;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.domain.scheduling.PublishingAttemptStatus;
import saas.personal_branding.api.domain.scheduling.PublishingJob;

@Component
@ConditionalOnProperty(value = "app.scheduling.worker.enabled", havingValue = "true")
public class PublishingWorker {

    private static final Logger log = LoggerFactory.getLogger(PublishingWorker.class);

    private final PublishingJobService publishingJobService;
    private final ReferenceDataRepository referenceDataRepository;
    private final PlatformPublisherRegistry publisherRegistry;
    private final PlatformCredentialService credentialService;
    private final ObjectMapper objectMapper;
    private final Counter claimedCounter;
    private final Counter successCounter;
    private final Counter failedCounter;
    private final int maxAttempts;
    private final Duration retryDelay;

    public PublishingWorker(PublishingJobService publishingJobService,
                            ReferenceDataRepository referenceDataRepository,
                            PlatformPublisherRegistry publisherRegistry,
                            PlatformCredentialService credentialService,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry,
                            @Value("${app.scheduling.worker.max-attempts:3}") int maxAttempts,
                            @Value("${app.scheduling.worker.retry-delay:PT5M}") Duration retryDelay) {
        this.publishingJobService = publishingJobService;
        this.referenceDataRepository = referenceDataRepository;
        this.publisherRegistry = publisherRegistry;
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
        this.claimedCounter = meterRegistry.counter("publishing.jobs.claimed");
        this.successCounter = meterRegistry.counter("publishing.jobs.success");
        this.failedCounter = meterRegistry.counter("publishing.jobs.failed");
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelay = retryDelay;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.worker.interval:PT15S}")
    public void poll() {
        List<PublishingJob> jobs = publishingJobService.claimDue(10);
        claimedCounter.increment(jobs.size());
        for (PublishingJob job : jobs) {
            try {
                Platform platform = resolvePlatform(job.getPlatformId());
                if (platform == null) {
                    handleFailure(job, "Unknown platform id: " + job.getPlatformId(), null);
                    continue;
                }

                PlatformConnection connection = credentialService.findConnectionById(job.getConnectionId()).orElse(null);
                if (connection == null) {
                    handleFailure(job, "Platform connection not found: " + job.getConnectionId(), null);
                    continue;
                }

                var authContext = credentialService.loadAuthContext(job.getConnectionId()).orElse(null);
                if (authContext == null) {
                    handleFailure(job, "No credentials stored for connection: " + job.getConnectionId(), null);
                    continue;
                }

                PlatformPublisher publisher = publisherRegistry.findPublisher(platform).orElse(null);
                if (publisher == null) {
                    handleFailure(job, "No publisher registered for platform: " + platform.getName(), null);
                    continue;
                }

                PlatformPublishCommand command = PlatformPublishCommand.builder()
                        .userId(job.getUserId())
                        .platform(platform)
                        .connection(connection)
                        .authContext(authContext)
                        .mediaAssetIds(job.getMediaAssetIds())
                        .caption(job.getCaption())
                        .scheduledAt(job.getScheduledAt())
                        .build();

                var result = publisher.publish(command);
                String providerResponse = serialize(result.getRawResponse());
                if (result.isSuccess()) {
                    publishingJobService.recordAttempt(job.getId(), PublishingAttemptStatus.SUCCESS, null, providerResponse);
                    if (result.getExternalPostId() != null) {
                        publishingJobService.setExternalPostId(job.getId(), result.getExternalPostId());
                    }
                    publishingJobService.markCompleted(job.getId());
                    successCounter.increment();
                } else {
                    String reason = result.getErrorMessage() != null ? result.getErrorMessage() : "Publish failed";
                    if (result.getErrorCode() != null) {
                        reason = result.getErrorCode() + ": " + reason;
                    }
                    handleFailure(job, reason, providerResponse);
                }
            } catch (Exception ex) {
                handleFailure(job, ex.getMessage(), null);
            }
        }
    }

    private String serialize(Object rawResponse) {
        if (rawResponse == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rawResponse);
        } catch (JsonProcessingException e) {
            return rawResponse.toString();
        }
    }

    private Platform resolvePlatform(Long platformId) {
        if (platformId == null) {
            return null;
        }
        return referenceDataRepository.findPlatformsByIds(Set.of(platformId)).stream().findFirst().orElse(null);
    }

    private void handleFailure(PublishingJob job, String reason, String providerResponse) {
        publishingJobService.recordAttempt(job.getId(), PublishingAttemptStatus.FAILURE, reason, providerResponse);
        if (job.getAttemptCount() >= maxAttempts) {
            publishingJobService.markDeadLetter(job.getId(), reason);
        } else {
            publishingJobService.markFailed(job.getId(), reason, retryDelay);
        }
        failedCounter.increment();
        log.warn("Publishing job failed id={} reason={}", job.getId(), reason);
    }
}
