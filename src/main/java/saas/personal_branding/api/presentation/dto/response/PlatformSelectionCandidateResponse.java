package saas.personal_branding.api.presentation.dto.response;

public record PlatformSelectionCandidateResponse(
        String primaryId,
        String primaryName,
        String secondaryId,
        String secondaryHandle,
        String secondaryName
) {
}
