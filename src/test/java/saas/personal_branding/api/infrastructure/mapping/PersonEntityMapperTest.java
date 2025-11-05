package saas.personal_branding.api.infrastructure.mapping;

import org.junit.jupiter.api.Test;
import saas.personal_branding.api.domain.model.Person;
import saas.personal_branding.api.infrastructure.persistence.entity.AudienceEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.CountryEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.NicheEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PersonEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PostingFrequencyEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.ToneEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersonEntityMapperTest {

    @Test
    void toDomainReturnsNullWhenEntityNull() {
        assertThat(PersonEntityMapper.toDomain(null)).isNull();
    }

    @Test
    void toDomainMapsScalarAndReferenceData() {
        PersonEntity entity = PersonEntity.builder()
                .id(7L)
                .user(UserEntity.builder().id(3L).build())
                .fullName("Jane Doe")
                .phoneNumber("+123456")
                .companyName("Acme Inc.")
                .position("CMO")
                .brandColor("#FFFFFF")
                .fontStyle("Lato")
                .niches(Set.of(createNiche(1L, "Tech")))
                .audiences(Set.of(createAudience(2L, "Founders")))
                .tones(Set.of(createTone(3L, "Friendly")))
                .platforms(Set.of(createPlatform(4L, "LinkedIn")))
                .countries(Set.of(createCountry(5L, "USA", "US")))
                .postingFrequencies(Set.of(createPostingFrequency(6L, "Daily")))
                .build();

        Person mapped = PersonEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(7L);
        assertThat(mapped.getUserId()).isEqualTo(3L);
        assertThat(mapped.getFullName()).isEqualTo("Jane Doe");
        assertThat(mapped.getBrandColor()).isEqualTo("#FFFFFF");
        assertThat(mapped.getFontStyle()).isEqualTo("Lato");
        assertThat(mapped.getNiches()).extracting("name").containsExactly("Tech");
        assertThat(mapped.getAudiences()).extracting("name").containsExactly("Founders");
        assertThat(mapped.getPlatforms()).extracting("name").containsExactly("LinkedIn");
        assertThat(mapped.getCountries()).extracting("isoCode").containsExactly("US");
        assertThat(mapped.getPostingFrequencies()).extracting("name").containsExactly("Daily");
    }

    private NicheEntity createNiche(Long id, String name) {
        NicheEntity entity = new NicheEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private AudienceEntity createAudience(Long id, String name) {
        AudienceEntity entity = new AudienceEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private ToneEntity createTone(Long id, String name) {
        ToneEntity entity = new ToneEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private PlatformEntity createPlatform(Long id, String name) {
        PlatformEntity entity = new PlatformEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private CountryEntity createCountry(Long id, String name, String isoCode) {
        CountryEntity entity = new CountryEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setIsoCode(isoCode);
        return entity;
    }

    private PostingFrequencyEntity createPostingFrequency(Long id, String name) {
        PostingFrequencyEntity entity = new PostingFrequencyEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
