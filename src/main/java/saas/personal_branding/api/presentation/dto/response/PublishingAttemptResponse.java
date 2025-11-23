package saas.personal_branding.api.presentation.dto.response;

import java.time.Instant;

public record PublishingAttemptResponse(Long id,
                                         Long jobId,
                                         Instant attemptedAt,
                                         String status,
                                         String error,
                                         String providerResponse) {
}
