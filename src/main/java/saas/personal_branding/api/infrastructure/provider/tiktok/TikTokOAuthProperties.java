package saas.personal_branding.api.infrastructure.provider.tiktok;

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
@ConfigurationProperties(prefix = "app.platform.tiktok")
public class TikTokOAuthProperties {

    @NotBlank
    private String clientKey;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;

    @NotNull
    private List<String> scopes = List.of("user.info.basic");

    @NotBlank
    private String authBaseUrl = "https://www.tiktok.com";

    @NotBlank
    private String apiBaseUrl = "https://open.tiktokapis.com";

    @NotBlank
    private String platformName = "TikTok";

    @NotNull
    private Duration stateTtl = Duration.ofMinutes(10);
}
