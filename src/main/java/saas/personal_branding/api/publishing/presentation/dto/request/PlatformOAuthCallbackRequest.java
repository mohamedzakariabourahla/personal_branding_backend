package saas.personal_branding.api.publishing.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformOAuthCallbackRequest {

    @NotBlank
    private String state;

    @NotBlank
    private String code;

    private String pageId;
}
