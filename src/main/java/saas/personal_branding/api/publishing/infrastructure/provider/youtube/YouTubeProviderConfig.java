package saas.personal_branding.api.publishing.infrastructure.provider.youtube;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import saas.personal_branding.api.application.port.out.youtube.YouTubeApiClient;

@Configuration
@Profile("!test")
public class YouTubeProviderConfig {

    @Bean
    public YouTubeApiClient youTubeApiClient(RestTemplate restTemplate, YouTubeOAuthProperties properties) {
        return new DefaultYouTubeApiClient(restTemplate, properties);
    }
}
