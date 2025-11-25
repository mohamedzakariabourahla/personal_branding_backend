package saas.personal_branding.api.publishing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.PlatformCredentialService;
import saas.personal_branding.api.application.service.PublishingJobService;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.publishing.domain.PublishingJob;

@Component
public class SchedulePublishingJobUseCase {

    private final PublishingJobService publishingJobService;
    private final PlatformCredentialService credentialService;
    private final ReferenceDataRepository referenceDataRepository;
    private final Clock clock;

    public SchedulePublishingJobUseCase(PublishingJobService publishingJobService,
                                        PlatformCredentialService credentialService,
                                        ReferenceDataRepository referenceDataRepository,
                                        Clock clock) {
        this.publishingJobService = publishingJobService;
        this.credentialService = credentialService;
        this.referenceDataRepository = referenceDataRepository;
        this.clock = clock;
    }

    public PublishingJob schedule(ScheduleCommand command) {
        Instant now = clock.instant();

        if (command.userId() == null) {
            throw new IllegalArgumentException("User id is required to schedule a job.");
        }
        if (command.platformId() == null) {
            throw new IllegalArgumentException("Platform id is required to schedule a job.");
        }
        if (command.connectionId() == null) {
            throw new IllegalArgumentException("Connection id is required to schedule a job.");
        }
        if (command.mediaAssetIds() == null || command.mediaAssetIds().isEmpty()) {
            throw new IllegalArgumentException("At least one media asset is required to schedule publishing.");
        }

        Platform platform = referenceDataRepository.findPlatformsByIds(Set.of(command.platformId()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + command.platformId()));

        PlatformConnection connection = credentialService.findConnectionById(command.connectionId())
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + command.connectionId()));

        if (!command.userId().equals(connection.getUserId())) {
            throw new IllegalArgumentException("Connection does not belong to the requesting user.");
        }
        if (connection.getPlatform() == null || !platform.getId().equals(connection.getPlatform().getId())) {
            throw new IllegalArgumentException("Connection is not linked to the requested platform.");
        }

        Instant scheduledAt = command.scheduledAt() != null ? command.scheduledAt() : now;
        if (scheduledAt.isBefore(now.minusSeconds(5))) {
            throw new IllegalArgumentException("Scheduled time must not be in the past.");
        }

        return publishingJobService.schedule(new PublishingJobService.ScheduleCommand(
                command.userId(),
                platform.getId(),
                connection.getId(),
                command.mediaAssetIds(),
                command.caption(),
                scheduledAt
        ));
    }

    public record ScheduleCommand(Long userId,
                                  Long platformId,
                                  Long connectionId,
                                  List<String> mediaAssetIds,
                                  String caption,
                                  Instant scheduledAt) {
    }
}
