package saas.personal_branding.api.infrastructure.provider.tiktok;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(TikTokOAuthProperties.class)
public class TikTokProviderConfig {

    @Bean
    public RestTemplate tikTokRestTemplate() {
        return new RestTemplate();
    }
}
