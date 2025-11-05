package saas.personal_branding.api.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class OnboardingRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotBlank
    @Size(max = 32)
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Size(max = 255)
    private String position;

    @NotBlank
    @Size(max = 32)
    private String brandColor;

    @NotBlank
    @Size(max = 128)
    private String fontStyle;

    @NotEmpty
    private Set<@NotNull @Positive Long> nicheIds;

    @NotEmpty
    private Set<@NotNull @Positive Long> audienceIds;

    @NotEmpty
    private Set<@NotNull @Positive Long> toneIds;

    @NotEmpty
    private Set<@NotNull @Positive Long> platformIds;

    @NotEmpty
    private Set<@NotNull @Positive Long> countryIds;

    @NotEmpty
    private Set<@NotNull @Positive Long> postingFrequencyIds;
}
