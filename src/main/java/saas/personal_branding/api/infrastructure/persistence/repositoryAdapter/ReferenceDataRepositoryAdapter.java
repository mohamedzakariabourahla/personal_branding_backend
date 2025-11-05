package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.*;
import saas.personal_branding.api.infrastructure.persistence.jpa.*;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        return loadAndMap(ids, jpaNicheRepository::findAllById, entity -> Niche.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Audience> findAudiencesByIds(Set<Long> ids) {
        return loadAndMap(ids, jpaAudienceRepository::findAllById, entity -> Audience.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Tone> findTonesByIds(Set<Long> ids) {
        return loadAndMap(ids, jpaToneRepository::findAllById, entity -> Tone.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Platform> findPlatformsByIds(Set<Long> ids) {
        return loadAndMap(ids, jpaPlatformRepository::findAllById, entity -> Platform.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Country> findCountriesByIds(Set<Long> ids) {
        return loadAndMap(ids, jpaCountryRepository::findAllById, entity -> Country.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isoCode(entity.getIsoCode())
                .build());
    }

    @Override
    public Set<PostingFrequency> findPostingFrequenciesByIds(Set<Long> ids) {
        return loadAndMap(ids, jpaPostingFrequencyRepository::findAllById, entity -> PostingFrequency.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Niche> findAllNiches() {
        return mapAll(jpaNicheRepository.findAll(), entity -> Niche.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Audience> findAllAudiences() {
        return mapAll(jpaAudienceRepository.findAll(), entity -> Audience.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Tone> findAllTones() {
        return mapAll(jpaToneRepository.findAll(), entity -> Tone.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Platform> findAllPlatforms() {
        return mapAll(jpaPlatformRepository.findAll(), entity -> Platform.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    @Override
    public Set<Country> findAllCountries() {
        return mapAll(jpaCountryRepository.findAll(), entity -> Country.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isoCode(entity.getIsoCode())
                .build());
    }

    @Override
    public Set<PostingFrequency> findAllPostingFrequencies() {
        return mapAll(jpaPostingFrequencyRepository.findAll(), entity -> PostingFrequency.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build());
    }

    private <E extends ReferenceDataEntity, D> Set<D> loadAndMap(Set<Long> ids,
                                                                 Function<Iterable<Long>, Iterable<E>> loader,
                                                                 Function<E, D> mapper) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        Iterable<E> entities = loader.apply(ids);
        return StreamSupport.stream(entities.spliterator(), false)
                .map(mapper)
                .collect(Collectors.toUnmodifiableSet());
    }

    private <E extends ReferenceDataEntity, D> Set<D> mapAll(Iterable<E> entities, Function<E, D> mapper) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(mapper)
                .collect(Collectors.toUnmodifiableSet());
    }
}
