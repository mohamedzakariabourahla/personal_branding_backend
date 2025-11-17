package saas.personal_branding.api.infrastructure.provider.youtube;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(YouTubeOAuthProperties.class)
public class YouTubeProviderConfig {

    @Bean
    public RestTemplate youTubeRestTemplate() {
        return new RestTemplate();
    }
}
