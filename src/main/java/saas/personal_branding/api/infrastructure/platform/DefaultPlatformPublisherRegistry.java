package saas.personal_branding.api.infrastructure.platform;

import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.PlatformPublisherRegistry;

import java.util.List;
import java.util.Optional;

public class DefaultPlatformPublisherRegistry implements PlatformPublisherRegistry {

    private final List<PlatformPublisher> publishers;

    public DefaultPlatformPublisherRegistry(List<PlatformPublisher> publishers) {
        this.publishers = publishers == null ? List.of() : List.copyOf(publishers);
    }

    @Override
    public Optional<PlatformPublisher> findPublisher(Platform platform) {
        if (platform == null) {
            return Optional.empty();
        }
        return publishers.stream()
                .filter(publisher -> publisher.supports(platform))
                .findFirst();
    }
}
