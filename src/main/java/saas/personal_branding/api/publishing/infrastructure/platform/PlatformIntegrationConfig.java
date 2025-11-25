package saas.personal_branding.api.publishing.infrastructure.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import saas.personal_branding.api.domain.platform.PlatformPublisher;

@Configuration
public class PlatformIntegrationConfig {

    @Bean
    @Profile("noop-platform")
    public PlatformPublisher noOpPlatformPublisher() {
        return new NoOpPlatformPublisher();
    }
}
