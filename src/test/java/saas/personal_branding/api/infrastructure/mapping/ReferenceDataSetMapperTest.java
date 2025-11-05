package saas.personal_branding.api.infrastructure.mapping;

import org.junit.jupiter.api.Test;
import saas.personal_branding.api.domain.model.Audience;
import saas.personal_branding.api.infrastructure.persistence.entity.AudienceEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.CountryEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.NicheEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PostingFrequencyEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.ToneEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceDataSetMapperTest {

    @Test
    void mapNichesReturnsEmptySetWhenInputNull() {
        assertThat(ReferenceDataSetMapper.mapNiches(null)).isEmpty();
    }

    @Test
    void mapAudiencesCreatesImmutableSet() {
        AudienceEntity entity = new AudienceEntity();
        entity.setId(1L);
        entity.setName("Founders");
        Set<AudienceEntity> entities = Set.of(entity);

        Set<Audience> mapped = ReferenceDataSetMapper.mapAudiences(entities);

        assertThat(mapped).hasSize(1);
        assertThat(mapped).extracting("name").containsExactly("Founders");
        Audience first = mapped.iterator().next();
        assertThatThrownBy(() -> mapped.add(first))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapCountriesCopiesIsoCode() {
        CountryEntity entity = new CountryEntity();
        entity.setId(2L);
        entity.setName("USA");
        entity.setIsoCode("US");

        assertThat(ReferenceDataSetMapper.mapCountries(Set.of(entity)))
                .extracting("isoCode")
                .containsExactly("US");
    }

    @Test
    void mapPlatformsTonesPostingFrequencies() {
        PlatformEntity platform = new PlatformEntity();
        platform.setId(3L);
        platform.setName("LinkedIn");
        ToneEntity tone = new ToneEntity();
        tone.setId(4L);
        tone.setName("Friendly");
        PostingFrequencyEntity frequency = new PostingFrequencyEntity();
        frequency.setId(5L);
        frequency.setName("Weekly");
        NicheEntity niche = new NicheEntity();
        niche.setId(6L);
        niche.setName("Tech");

        assertThat(ReferenceDataSetMapper.mapPlatforms(Set.of(platform))).extracting("name").containsExactly("LinkedIn");
        assertThat(ReferenceDataSetMapper.mapTones(Set.of(tone))).extracting("name").containsExactly("Friendly");
        assertThat(ReferenceDataSetMapper.mapPostingFrequencies(Set.of(frequency))).extracting("name").containsExactly("Weekly");
        assertThat(ReferenceDataSetMapper.mapNiches(Set.of(niche))).extracting("name").containsExactly("Tech");
    }
}
