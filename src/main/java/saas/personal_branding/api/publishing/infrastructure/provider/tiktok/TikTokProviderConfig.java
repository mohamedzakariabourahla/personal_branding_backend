package saas.personal_branding.api.publishing.infrastructure.provider.tiktok;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import saas.personal_branding.api.application.port.out.tiktok.TikTokApiClient;

@Configuration
@Profile("!test")
public class TikTokProviderConfig {

    @Bean
    public TikTokApiClient tikTokApiClient(RestTemplate restTemplate, TikTokOAuthProperties properties) {
        return new DefaultTikTokApiClient(restTemplate, properties);
    }
}
