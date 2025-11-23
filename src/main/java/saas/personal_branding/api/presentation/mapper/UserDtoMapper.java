package saas.personal_branding.api.presentation.mapper;

import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.presentation.dto.response.*;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserDtoMapper {

    private UserDtoMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        PersonResponse personResponse = toPersonResponse(user.getPerson());

        Set<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isActive(),
                user.isEmailVerified(),
                user.getOnboardingStatus(),
                roles,
                personResponse
        );
    }

    public static PersonResponse toPersonResponse(Person person) {
        if (person == null) {
            return null;
        }

        return new PersonResponse(
                person.getId(),
                person.getUserId(),
                person.getFullName(),
                person.getPhoneNumber(),
                person.getCompanyName(),
                person.getPosition(),
                person.getBrandColor(),
                person.getFontStyle(),
                toReferenceResponse(person.getNiches()),
                toReferenceResponse(person.getAudiences()),
                toReferenceResponse(person.getTones()),
                toReferenceResponse(person.getPlatforms()),
                toCountryResponse(person.getCountries()),
                toReferenceResponse(person.getPostingFrequencies())
        );
    }

    public static PersonResponse emptyPersonResponse(Long userId) {
        return new PersonResponse(
                null,
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    private static Set<ReferenceDataResponse> toReferenceResponse(Set<?> items) {
        if (items == null || items.isEmpty()) {
            return Set.of();
        }

        return items.stream()
                .map(item -> {
                    if (item instanceof Niche niche) {
                        return new ReferenceDataResponse(niche.getId(), niche.getName());
                    }
                    if (item instanceof Audience audience) {
                        return new ReferenceDataResponse(audience.getId(), audience.getName());
                    }
                    if (item instanceof Tone tone) {
                        return new ReferenceDataResponse(tone.getId(), tone.getName());
                    }
                    if (item instanceof Platform platform) {
                        return new ReferenceDataResponse(platform.getId(), platform.getName());
                    }
                    if (item instanceof PostingFrequency postingFrequency) {
                        return new ReferenceDataResponse(postingFrequency.getId(), postingFrequency.getName());
                    }
                    throw new IllegalArgumentException("Unsupported reference data type: " + item.getClass());
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<CountryResponse> toCountryResponse(Set<Country> countries) {
        if (countries == null || countries.isEmpty()) {
            return Set.of();
        }

        return countries.stream()
                .map(country -> new CountryResponse(country.getId(), country.getName(), country.getIsoCode()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static AuthResponse toAuthResponse(AuthService.AuthResult authResult) {
        return new AuthResponse(
                toUserResponse(authResult.user()),
                new TokenResponse(
                        authResult.accessToken(),
                        "Bearer",
                        authResult.deviceId(),
                        authResult.deviceName()
                )
        );
    }
}
