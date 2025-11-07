package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.Audience;
import saas.personal_branding.api.domain.model.Country;
import saas.personal_branding.api.domain.model.Niche;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PostingFrequency;
import saas.personal_branding.api.domain.model.Tone;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper;
import saas.personal_branding.api.infrastructure.persistence.entity.*;
import saas.personal_branding.api.infrastructure.persistence.jpa.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapAudiences;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapCountries;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapNiches;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapPlatforms;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapPlatform;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapPostingFrequencies;
import static saas.personal_branding.api.infrastructure.mapping.ReferenceDataSetMapper.mapTones;

@Repository
@Transactional(readOnly = true)
public class ReferenceDataRepositoryAdapter implements ReferenceDataRepository {

    private final JpaNicheRepository jpaNicheRepository;
    private final JpaAudienceRepository jpaAudienceRepository;
    private final JpaToneRepository jpaToneRepository;
    private final JpaPlatformRepository jpaPlatformRepository;
    private final JpaCountryRepository jpaCountryRepository;
    private final JpaPostingFrequencyRepository jpaPostingFrequencyRepository;

    public ReferenceDataRepositoryAdapter(JpaNicheRepository jpaNicheRepository,
                                          JpaAudienceRepository jpaAudienceRepository,
                                          JpaToneRepository jpaToneRepository,
                                          JpaPlatformRepository jpaPlatformRepository,
                                          JpaCountryRepository jpaCountryRepository,
                                          JpaPostingFrequencyRepository jpaPostingFrequencyRepository) {
        this.jpaNicheRepository = jpaNicheRepository;
        this.jpaAudienceRepository = jpaAudienceRepository;
        this.jpaToneRepository = jpaToneRepository;
        this.jpaPlatformRepository = jpaPlatformRepository;
        this.jpaCountryRepository = jpaCountryRepository;
        this.jpaPostingFrequencyRepository = jpaPostingFrequencyRepository;
    }

    @Override
    public Set<Niche> findNichesByIds(Set<Long> ids) {
        return mapNiches(loadEntities(ids, jpaNicheRepository::findAllById));
    }

    @Override
    public Set<Audience> findAudiencesByIds(Set<Long> ids) {
        return mapAudiences(loadEntities(ids, jpaAudienceRepository::findAllById));
    }

    @Override
    public Set<Tone> findTonesByIds(Set<Long> ids) {
        return mapTones(loadEntities(ids, jpaToneRepository::findAllById));
    }

    @Override
    public Set<Platform> findPlatformsByIds(Set<Long> ids) {
        return mapPlatforms(loadEntities(ids, jpaPlatformRepository::findAllById));
    }

    @Override
    public Set<Country> findCountriesByIds(Set<Long> ids) {
        return mapCountries(loadEntities(ids, jpaCountryRepository::findAllById));
    }

    @Override
    public Set<PostingFrequency> findPostingFrequenciesByIds(Set<Long> ids) {
        return mapPostingFrequencies(loadEntities(ids, jpaPostingFrequencyRepository::findAllById));
    }

    @Override
    public Set<Niche> findAllNiches() {
        return mapNiches(toSet(jpaNicheRepository.findAll()));
    }

    @Override
    public Set<Audience> findAllAudiences() {
        return mapAudiences(toSet(jpaAudienceRepository.findAll()));
    }

    @Override
    public Set<Tone> findAllTones() {
        return mapTones(toSet(jpaToneRepository.findAll()));
    }

    @Override
    public Set<Platform> findAllPlatforms() {
        return mapPlatforms(toSet(jpaPlatformRepository.findAll()));
    }

    @Override
    public Set<Country> findAllCountries() {
        return mapCountries(toSet(jpaCountryRepository.findAll()));
    }

    @Override
    public Set<PostingFrequency> findAllPostingFrequencies() {
        return mapPostingFrequencies(toSet(jpaPostingFrequencyRepository.findAll()));
    }

    @Override
    public java.util.Optional<Platform> findPlatformByName(String name) {
        if (name == null || name.isBlank()) {
            return java.util.Optional.empty();
        }
        return jpaPlatformRepository.findByNameIgnoreCase(name.trim())
                .map(ReferenceDataSetMapper::mapPlatform);
    }

    private <E extends ReferenceDataEntity> Set<E> loadEntities(Set<Long> ids,
                                                                 Function<Iterable<Long>, Iterable<E>> loader) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        return toSet(loader.apply(ids));
    }

    private <E extends ReferenceDataEntity> Set<E> toSet(Iterable<E> iterable) {
        if (iterable instanceof Set<E> asSet) {
            return Set.copyOf(asSet);
        }
        if (iterable instanceof java.util.Collection<E> collection) {
            return Set.copyOf(collection);
        }
        Set<E> result = new HashSet<>();
        iterable.forEach(result::add);
        return Set.copyOf(result);
    }
}
