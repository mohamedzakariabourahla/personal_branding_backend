package saas.personal_branding.api.domain.platform;

import saas.personal_branding.api.domain.model.Platform;

import java.util.Optional;

public interface PlatformPublisherRegistry {

    Optional<PlatformPublisher> findPublisher(Platform platform);

    default PlatformPublisher requirePublisher(Platform platform) {
        return findPublisher(platform)
                .orElseThrow(() -> new IllegalArgumentException("No publisher registered for platform: " + platform.getName()));
    }
}
