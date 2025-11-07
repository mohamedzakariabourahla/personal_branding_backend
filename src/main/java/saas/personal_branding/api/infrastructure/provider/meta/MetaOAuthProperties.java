package saas.personal_branding.api.infrastructure.provider.meta;

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
@ConfigurationProperties(prefix = "app.platform.meta")
public class MetaOAuthProperties {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;

    @NotNull
    private List<String> scopes = List.of("pages_show_list", "instagram_basic");

    @NotBlank
    private String apiVersion = "v20.0";

    @NotBlank
    private String authBaseUrl = "https://www.facebook.com";

    @NotBlank
    private String graphBaseUrl = "https://graph.facebook.com";

    @NotBlank
    private String platformName = "Instagram";

    @NotNull
    private Duration stateTtl = Duration.ofMinutes(10);
}
