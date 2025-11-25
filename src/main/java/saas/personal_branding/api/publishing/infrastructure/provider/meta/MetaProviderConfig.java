package saas.personal_branding.api.publishing.infrastructure.provider.meta;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import saas.personal_branding.api.application.port.out.meta.MetaGraphClient;

@Configuration
@Profile("!test")
public class MetaProviderConfig {

    @Bean
    public MetaGraphClient metaGraphClient(RestTemplate restTemplate,
                                           com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                           MetaOAuthProperties properties) {
        return new DefaultMetaGraphClient(restTemplate, objectMapper, properties);
    }
}
