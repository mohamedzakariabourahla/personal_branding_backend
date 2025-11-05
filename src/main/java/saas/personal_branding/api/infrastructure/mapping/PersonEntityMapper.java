package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.Person;
import saas.personal_branding.api.infrastructure.persistence.entity.PersonEntity;

public final class PersonEntityMapper {

    private PersonEntityMapper() {
    }

    public static Person toDomain(PersonEntity entity) {
        if (entity == null) {
            return null;
        }

        return Person.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .companyName(entity.getCompanyName())
                .position(entity.getPosition())
                .brandColor(entity.getBrandColor())
                .fontStyle(entity.getFontStyle())
                .niches(ReferenceDataSetMapper.mapNiches(entity.getNiches()))
                .audiences(ReferenceDataSetMapper.mapAudiences(entity.getAudiences()))
                .tones(ReferenceDataSetMapper.mapTones(entity.getTones()))
                .platforms(ReferenceDataSetMapper.mapPlatforms(entity.getPlatforms()))
                .countries(ReferenceDataSetMapper.mapCountries(entity.getCountries()))
                .postingFrequencies(ReferenceDataSetMapper.mapPostingFrequencies(entity.getPostingFrequencies()))
                .build();
    }
}
