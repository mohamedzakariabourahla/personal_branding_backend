package saas.personal_branding.api.publishing.infrastructure.platform;

import java.time.Instant;
import java.util.Map;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.platform.PlatformPublishCommand;
import saas.personal_branding.api.domain.platform.PlatformPublishResult;
import saas.personal_branding.api.domain.platform.PlatformPublisher;

public class NoOpPlatformPublisher implements PlatformPublisher {

    @Override
    public boolean supports(Platform platform) {
        return true;
    }

    @Override
    public PlatformPublishResult publish(PlatformPublishCommand command) {
        return PlatformPublishResult.success("noop-" + command.getUserId(), Instant.now(), Map.of());
    }
}
