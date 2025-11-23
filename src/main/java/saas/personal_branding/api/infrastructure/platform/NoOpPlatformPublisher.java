package saas.personal_branding.api.infrastructure.platform;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.platform.PlatformPublishCommand;
import saas.personal_branding.api.domain.platform.PlatformPublishResult;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.Platforms;

/**
 * Placeholder publisher until provider-specific implementations are wired.
 * Returns a failure with a clear message but allows the worker pipeline to run and record attempts.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NoOpPlatformPublisher implements PlatformPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpPlatformPublisher.class);

    private static final Set<String> SUPPORTED = Set.of(Platforms.TIKTOK, Platforms.YOUTUBE);
    private static final Set<String> SUPPORTED_CODES = Set.of(Platforms.TIKTOK_CODE, Platforms.YOUTUBE_CODE);

    @Override
    public boolean supports(Platform platform) {
        if (platform == null) {
            return false;
        }
        if (platform.getCode() != null && SUPPORTED_CODES.contains(platform.getCode())) {
            return true;
        }
        return platform.getName() != null && SUPPORTED.contains(platform.getName());
    }

    @Override
    public PlatformPublishResult publish(PlatformPublishCommand command) {
        String platformName = command.getPlatform().getName();
        log.warn("Publishing not yet implemented for platform {}", platformName);
        return PlatformPublishResult.builder()
                .success(false)
                .errorCode("PUBLISH_NOT_IMPLEMENTED")
                .errorMessage("Publishing not implemented for platform " + platformName)
                .publishedAt(Instant.now())
                .rawResponse(Map.of("status", "not_implemented"))
                .build();
    }
}
