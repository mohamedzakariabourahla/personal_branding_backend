package saas.personal_branding.api.infrastructure.provider.youtube;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.platform.youtube")
public class YouTubeOAuthProperties {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;

    @NotNull
    private List<String> scopes = List.of("https://www.googleapis.com/auth/youtube.readonly");

    @NotBlank
    private String authBaseUrl = "https://accounts.google.com";

    @NotBlank
    private String tokenUrl = "https://oauth2.googleapis.com/token";

    @NotBlank
    private String apiBaseUrl = "https://www.googleapis.com";

    @NotBlank
    private String platformName = "YouTube";

    @NotNull
    private Duration stateTtl = Duration.ofMinutes(10);
}
