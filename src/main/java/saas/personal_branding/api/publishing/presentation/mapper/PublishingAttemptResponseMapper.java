package saas.personal_branding.api.publishing.presentation.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.application.service.FailureReasonNormalizer;
import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.presentation.dto.response.PublishingAttemptResponse;

@Component
public class PublishingAttemptResponseMapper {

    private final FailureReasonNormalizer failureReasonNormalizer;

    public PublishingAttemptResponseMapper(FailureReasonNormalizer failureReasonNormalizer) {
        this.failureReasonNormalizer = failureReasonNormalizer;
    }

    public PublishingAttemptResponse toResponse(PublishingAttempt attempt) {
        return new PublishingAttemptResponse(
                attempt.getId(),
                attempt.getJobId(),
                attempt.getAttemptedAt(),
                attempt.getStatus().name(),
                attempt.getError(),
                failureReasonNormalizer.toUserMessage(
                        attempt.getError() != null ? attempt.getError() : attempt.getProviderResponse())
        );
    }

    public List<PublishingAttemptResponse> toResponseList(List<PublishingAttempt> attempts) {
        return attempts.stream().map(this::toResponse).toList();
    }
}
