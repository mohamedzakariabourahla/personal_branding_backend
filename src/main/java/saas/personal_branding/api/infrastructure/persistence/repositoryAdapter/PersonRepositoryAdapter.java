package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.Audience;
import saas.personal_branding.api.domain.model.Country;
import saas.personal_branding.api.domain.model.Niche;
import saas.personal_branding.api.domain.model.Person;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PostingFrequency;
import saas.personal_branding.api.domain.model.Tone;
import saas.personal_branding.api.domain.repository.PersonRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.*;
import saas.personal_branding.api.infrastructure.persistence.jpa.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import saas.personal_branding.api.infrastructure.mapping.PersonEntityMapper;

@Repository
@Transactional
public class PersonRepositoryAdapter implements PersonRepository {

    private final JpaPersonRepository jpaPersonRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaNicheRepository jpaNicheRepository;
    private final JpaAudienceRepository jpaAudienceRepository;
    private final JpaToneRepository jpaToneRepository;
    private final JpaPlatformRepository jpaPlatformRepository;
    private final JpaCountryRepository jpaCountryRepository;
    private final JpaPostingFrequencyRepository jpaPostingFrequencyRepository;

    public PersonRepositoryAdapter(JpaPersonRepository jpaPersonRepository,
                                   JpaUserRepository jpaUserRepository,
                                   JpaNicheRepository jpaNicheRepository,
                                   JpaAudienceRepository jpaAudienceRepository,
                                   JpaToneRepository jpaToneRepository,
                                   JpaPlatformRepository jpaPlatformRepository,
                                   JpaCountryRepository jpaCountryRepository,
                                   JpaPostingFrequencyRepository jpaPostingFrequencyRepository) {
        this.jpaPersonRepository = jpaPersonRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaNicheRepository = jpaNicheRepository;
        this.jpaAudienceRepository = jpaAudienceRepository;
        this.jpaToneRepository = jpaToneRepository;
        this.jpaPlatformRepository = jpaPlatformRepository;
        this.jpaCountryRepository = jpaCountryRepository;
        this.jpaPostingFrequencyRepository = jpaPostingFrequencyRepository;
    }

    @Override
    public Person save(Person person) {
        PersonEntity entity = person.getId() != null
                ? jpaPersonRepository.findById(person.getId()).orElse(new PersonEntity())
                : new PersonEntity();

        UserEntity userEntity = jpaUserRepository.findById(person.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + person.getUserId()));

        entity.setUser(userEntity);
        entity.setFullName(person.getFullName());
        entity.setPhoneNumber(person.getPhoneNumber());
        entity.setCompanyName(person.getCompanyName());
        entity.setPosition(person.getPosition());
        entity.setBrandColor(person.getBrandColor());
        entity.setFontStyle(person.getFontStyle());

        entity.setNiches(loadNiches(person.getNiches()));
        entity.setAudiences(loadAudiences(person.getAudiences()));
        entity.setTones(loadTones(person.getTones()));
        entity.setPlatforms(loadPlatforms(person.getPlatforms()));
        entity.setCountries(loadCountries(person.getCountries()));
        entity.setPostingFrequencies(loadPostingFrequencies(person.getPostingFrequencies()));

        PersonEntity saved = jpaPersonRepository.save(entity);
        userEntity.setPerson(saved);

        return PersonEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Person> findByUserId(Long userId) {
        return jpaPersonRepository.findByUserId(userId)
                .map(PersonEntityMapper::toDomain);
    }

    private Set<NicheEntity> loadNiches(Set<Niche> niches) {
        return loadEntities(niches, Niche::getId, jpaNicheRepository::findAllById);
    }

    private Set<AudienceEntity> loadAudiences(Set<Audience> audiences) {
        return loadEntities(audiences, Audience::getId, jpaAudienceRepository::findAllById);
    }

    private Set<ToneEntity> loadTones(Set<Tone> tones) {
        return loadEntities(tones, Tone::getId, jpaToneRepository::findAllById);
    }

    private Set<PlatformEntity> loadPlatforms(Set<Platform> platforms) {
        return loadEntities(platforms, Platform::getId, jpaPlatformRepository::findAllById);
    }

    private Set<CountryEntity> loadCountries(Set<Country> countries) {
        return loadEntities(countries, Country::getId, jpaCountryRepository::findAllById);
    }

    private Set<PostingFrequencyEntity> loadPostingFrequencies(Set<PostingFrequency> postingFrequencies) {
        return loadEntities(postingFrequencies, PostingFrequency::getId, jpaPostingFrequencyRepository::findAllById);
    }

    private <D, E> Set<E> loadEntities(Set<D> domainObjects,
                                       Function<D, Long> idExtractor,
                                       Function<Iterable<Long>, Iterable<E>> loader) {
        if (domainObjects == null || domainObjects.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> ids = domainObjects.stream()
                .map(idExtractor)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Iterable<E> found = loader.apply(ids);
        Set<E> result = new HashSet<>();
        found.forEach(result::add);
        return result;
    }
}
