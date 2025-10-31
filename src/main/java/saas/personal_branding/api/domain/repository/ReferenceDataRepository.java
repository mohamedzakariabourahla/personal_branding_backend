package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.*;

import java.util.Set;

public interface ReferenceDataRepository {
    Set<Niche> findNichesByIds(Set<Long> ids);
    Set<Audience> findAudiencesByIds(Set<Long> ids);
    Set<Tone> findTonesByIds(Set<Long> ids);
    Set<Platform> findPlatformsByIds(Set<Long> ids);
    Set<Country> findCountriesByIds(Set<Long> ids);
    Set<PostingFrequency> findPostingFrequenciesByIds(Set<Long> ids);
}
