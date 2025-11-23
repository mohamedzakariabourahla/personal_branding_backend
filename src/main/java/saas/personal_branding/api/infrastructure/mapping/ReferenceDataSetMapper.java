package saas.personal_branding.api.infrastructure.mapping;

import java.util.Set;
import java.util.stream.Collectors;

import saas.personal_branding.api.domain.model.Audience;
import saas.personal_branding.api.domain.model.Country;
import saas.personal_branding.api.domain.model.Niche;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PostingFrequency;
import saas.personal_branding.api.domain.model.Tone;
import saas.personal_branding.api.infrastructure.persistence.entity.AudienceEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.CountryEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.NicheEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PostingFrequencyEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.ToneEntity;

public final class ReferenceDataSetMapper {

    private ReferenceDataSetMapper() {
    }

    public static Set<Niche> mapNiches(Set<NicheEntity> entities) {
        return mapReference(entities, entity -> Niche.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    public static Set<Audience> mapAudiences(Set<AudienceEntity> entities) {
        return mapReference(entities, entity -> Audience.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    public static Set<Tone> mapTones(Set<ToneEntity> entities) {
        return mapReference(entities, entity -> Tone.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    public static Set<Platform> mapPlatforms(Set<PlatformEntity> entities) {
        return mapReference(entities, entity -> Platform.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .build());
    }

    public static Platform mapPlatform(PlatformEntity entity) {
        if (entity == null) {
            return null;
        }
        return Platform.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .build();
    }

    public static Set<Country> mapCountries(Set<CountryEntity> entities) {
        return mapReference(entities, entity -> Country.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isoCode(entity.getIsoCode())
                .build());
    }

    public static Set<PostingFrequency> mapPostingFrequencies(Set<PostingFrequencyEntity> entities) {
        return mapReference(entities, entity -> PostingFrequency.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    private static <E, T> Set<T> mapReference(Set<E> entities, java.util.function.Function<E, T> mapper) {
        if (entities == null || entities.isEmpty()) {
            return Set.of();
        }
        return entities.stream()
                .map(mapper)
                .collect(Collectors.toUnmodifiableSet());
    }
}
