package saas.personal_branding.api.presentation.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class OnboardingRequest {
    private String fullName;
    private String phoneNumber;
    private String companyName;
    private String position;
    private String brandColor;
    private String fontStyle;
    private Set<Long> nicheIds;
    private Set<Long> audienceIds;
    private Set<Long> toneIds;
    private Set<Long> platformIds;
    private Set<Long> countryIds;
    private Set<Long> postingFrequencyIds;
}
