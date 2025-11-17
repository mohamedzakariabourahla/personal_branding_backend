package saas.personal_branding.api.application.exception;

import java.util.List;

public abstract class PlatformException extends BusinessException {

    protected PlatformException(String message, String errorCode) {
        super(message, errorCode);
    }

    public static class NoEligibleAccountException extends PlatformException {
        public NoEligibleAccountException() {
            super("No eligible account is linked to this profile.", "platform.no_account");
        }
    }

    public static class InvalidAccountSelectionException extends PlatformException {
        public InvalidAccountSelectionException(String pageId) {
            super("The selected account is no longer available. Please refresh and try again. (pageId=" + pageId + ")", "platform.invalid_selection");
        }
    }

    public static class SelectionRequiredException extends PlatformException {
        private final List<PlatformSelectionCandidate> candidates;

        public SelectionRequiredException(List<PlatformSelectionCandidate> candidates) {
            super("Multiple accounts were found. Select which one to connect.", "platform.selection.required");
            this.candidates = candidates;
        }

        public List<PlatformSelectionCandidate> getCandidates() {
            return candidates;
        }
    }

    public static class ProviderCommunicationException extends PlatformException {
        public ProviderCommunicationException(String message) {
            super(message, "platform.provider.unavailable");
        }
    }

    public record PlatformSelectionCandidate(String primaryId,
                                             String primaryName,
                                             String secondaryId,
                                             String secondaryHandle,
                                             String secondaryName) {
    }
}
