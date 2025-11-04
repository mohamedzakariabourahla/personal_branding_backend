package saas.personal_branding.api.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetInitiateRequest {

    @NotBlank
    @Email
    private String email;
}
