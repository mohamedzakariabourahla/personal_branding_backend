package saas.personal_branding.api.presentation.dto.response;

import java.util.List;

public record OAuthCompletionResult(
        OAuthCompletionStatus status,
        PlatformConnectionResponse connection,
        List<PlatformSelectionCandidateResponse> candidates,
        String message
) {

    public static OAuthCompletionResult connected(PlatformConnectionResponse connection) {
        return new OAuthCompletionResult(OAuthCompletionStatus.CONNECTED, connection, List.of(), null);
    }

    public static OAuthCompletionResult selectionRequired(List<PlatformSelectionCandidateResponse> candidates) {
        return new OAuthCompletionResult(OAuthCompletionStatus.SELECTION_REQUIRED, null, candidates, null);
    }

    public static OAuthCompletionResult failure(String message) {
        return new OAuthCompletionResult(OAuthCompletionStatus.FAILED, null, List.of(), message);
    }
}

