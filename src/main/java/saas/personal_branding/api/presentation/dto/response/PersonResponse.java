package saas.personal_branding.api.presentation.dto.response;

import java.util.Set;

public record PersonResponse(Long id,
                             Long userId,
                             String fullName,
                             String phoneNumber,
                             String companyName,
                             String position,
                             String brandColor,
                             String fontStyle,
                             Set<ReferenceDataResponse> niches,
                             Set<ReferenceDataResponse> audiences,
                             Set<ReferenceDataResponse> tones,
                             Set<ReferenceDataResponse> platforms,
                             Set<CountryResponse> countries,
                             Set<ReferenceDataResponse> postingFrequencies) {
}
