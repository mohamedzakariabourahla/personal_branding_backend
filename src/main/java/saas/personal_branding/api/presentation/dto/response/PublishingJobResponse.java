package saas.personal_branding.api.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import saas.personal_branding.api.domain.scheduling.PublishingJobStatus;

public record PublishingJobResponse(Long id,
                                    Long platformId,
                                    Long connectionId,
                                    List<String> mediaAssetIds,
                                    String caption,
                                    Instant scheduledAt,
                                    Instant createdAt,
                                    Instant lastTriedAt,
                                    Instant completedAt,
                                    int attemptCount,
                                    PublishingJobStatus status,
                                    String failureReason,
                                    String failureUserMessage,
                                    String externalPostId) {
}
