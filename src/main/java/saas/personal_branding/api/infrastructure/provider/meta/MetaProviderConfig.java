package saas.personal_branding.api.infrastructure.provider.meta;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(MetaOAuthProperties.class)
public class MetaProviderConfig {

    @Bean
    public RestTemplate metaRestTemplate() {
        return new RestTemplate();
    }
}
