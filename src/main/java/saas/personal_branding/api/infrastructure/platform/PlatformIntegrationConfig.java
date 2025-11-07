package saas.personal_branding.api.infrastructure.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.PlatformPublisherRegistry;

import java.util.List;

@Configuration
public class PlatformIntegrationConfig {

    @Bean
    public PlatformPublisherRegistry platformPublisherRegistry(List<PlatformPublisher> publishers) {
        return new DefaultPlatformPublisherRegistry(publishers);
    }
}
