package saas.personal_branding.api.publishing.infrastructure.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.PlatformPublisherRegistry;

@Component
public class DefaultPlatformPublisherRegistry implements PlatformPublisherRegistry {

    private final Map<String, PlatformPublisher> publishersByCode = new HashMap<>();

    public DefaultPlatformPublisherRegistry(Optional<PlatformPublisher> defaultPublisher,
                                            Optional<PlatformPublisher> noOpPublisher,
                                            Optional<PlatformPublisher> instagramPublisher,
                                            Optional<PlatformPublisher> tiktokPublisher,
                                            Optional<PlatformPublisher> youTubePublisher) {
        defaultPublisher.ifPresent(p -> publishersByCode.put("default", p));
        noOpPublisher.ifPresent(p -> publishersByCode.put("noop", p));
        instagramPublisher.ifPresent(p -> publishersByCode.put("instagram", p));
        tiktokPublisher.ifPresent(p -> publishersByCode.put("tiktok", p));
        youTubePublisher.ifPresent(p -> publishersByCode.put("youtube", p));
    }

    @Override
    public Optional<PlatformPublisher> findPublisher(Platform platform) {
        if (platform == null || platform.getCode() == null) {
            return Optional.ofNullable(publishersByCode.get("noop"));
        }
        return Optional.ofNullable(publishersByCode.getOrDefault(platform.getCode(), publishersByCode.get("noop")));
    }
}
