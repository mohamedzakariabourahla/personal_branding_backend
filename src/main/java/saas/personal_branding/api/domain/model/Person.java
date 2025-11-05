package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import java.util.Collections;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class Person {

    private final Long id;
    private final Long userId;
    private final String fullName;
    private final String phoneNumber;
    private final String companyName;
    private final String position;
    private final String brandColor;
    private final String fontStyle;

    @Singular("niche")
    private final Set<Niche> niches;

    @Singular("audience")
    private final Set<Audience> audiences;

    @Singular("tone")
    private final Set<Tone> tones;

    @Singular("platform")
    private final Set<Platform> platforms;

    @Singular("country")
    private final Set<Country> countries;

    @Singular("postingFrequency")
    private final Set<PostingFrequency> postingFrequencies;

    public Set<Niche> getNiches() {
        return niches == null ? Collections.emptySet() : Collections.unmodifiableSet(niches);
    }

    public Set<Audience> getAudiences() {
        return audiences == null ? Collections.emptySet() : Collections.unmodifiableSet(audiences);
    }

    public Set<Tone> getTones() {
        return tones == null ? Collections.emptySet() : Collections.unmodifiableSet(tones);
    }

    public Set<Platform> getPlatforms() {
        return platforms == null ? Collections.emptySet() : Collections.unmodifiableSet(platforms);
    }

    public Set<Country> getCountries() {
        return countries == null ? Collections.emptySet() : Collections.unmodifiableSet(countries);
    }

    public Set<PostingFrequency> getPostingFrequencies() {
        return postingFrequencies == null ? Collections.emptySet() : Collections.unmodifiableSet(postingFrequencies);
    }
}
