package saas.personal_branding.api.domain.platform;

import saas.personal_branding.api.domain.model.Platform;

public interface PlatformPublisher {

    /**
     * Returns true if this publisher can handle the given platform entry (e.g., Instagram).
     */
    boolean supports(Platform platform);

    /**
     * Publishes the requested content to the underlying platform.
     */
    PlatformPublishResult publish(PlatformPublishCommand command);
}
