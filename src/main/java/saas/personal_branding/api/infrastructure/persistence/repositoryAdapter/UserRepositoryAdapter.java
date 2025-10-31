package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.domain.repository.UserRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.*;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.getId() != null
                ? jpaUserRepository.findById(user.getId()).orElse(new UserEntity())
                : new UserEntity();

        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setActive(user.isActive());
        entity.setOnboardingStatus(user.getOnboardingStatus());
        entity.setRoles(user.getRoles().isEmpty() ? new HashSet<>() : new HashSet<>(user.getRoles()));

        UserEntity saved = jpaUserRepository.save(entity);
        return jpaUserRepository.findDetailedById(saved.getId())
                .map(this::toDomain)
                .orElseGet(() -> toDomain(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpaUserRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findDetailedById(id)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public User updateOnboardingStatus(Long userId, OnboardingStatus status) {
        UserEntity entity = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        entity.setOnboardingStatus(status);
        UserEntity saved = jpaUserRepository.save(entity);

        return jpaUserRepository.findDetailedById(saved.getId())
                .map(this::toDomain)
                .orElseGet(() -> toDomain(saved));
    }

    private User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        Person person = toDomain(entity.getPerson());

        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .active(Boolean.TRUE.equals(entity.getActive()))
                .onboardingStatus(entity.getOnboardingStatus())
                .roles(entity.getRoles() == null ? Set.of() : Set.copyOf(entity.getRoles()))
                .person(person)
                .build();
    }

    private Person toDomain(PersonEntity entity) {
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
                .niches(mapNiches(entity.getNiches()))
                .audiences(mapAudiences(entity.getAudiences()))
                .tones(mapTones(entity.getTones()))
                .platforms(mapPlatforms(entity.getPlatforms()))
                .countries(mapCountries(entity.getCountries()))
                .postingFrequencies(mapPostingFrequencies(entity.getPostingFrequencies()))
                .build();
    }

    private Set<Niche> mapNiches(Set<NicheEntity> entities) {
        return mapReference(entities, e -> Niche.builder()
                .id(e.getId())
                .name(e.getName())
                .build());
    }

    private Set<Audience> mapAudiences(Set<AudienceEntity> entities) {
        return mapReference(entities, e -> Audience.builder()
                .id(e.getId())
                .name(e.getName())
                .build());
    }

    private Set<Tone> mapTones(Set<ToneEntity> entities) {
        return mapReference(entities, e -> Tone.builder()
                .id(e.getId())
                .name(e.getName())
                .build());
    }

    private Set<Platform> mapPlatforms(Set<PlatformEntity> entities) {
        return mapReference(entities, e -> Platform.builder()
                .id(e.getId())
                .name(e.getName())
                .build());
    }

    private Set<Country> mapCountries(Set<CountryEntity> entities) {
        return mapReference(entities, e -> Country.builder()
                .id(e.getId())
                .name(e.getName())
                .isoCode(e.getIsoCode())
                .build());
    }

    private Set<PostingFrequency> mapPostingFrequencies(Set<PostingFrequencyEntity> entities) {
        return mapReference(entities, e -> PostingFrequency.builder()
                .id(e.getId())
                .name(e.getName())
                .build());
    }

    private <E extends ReferenceDataEntity, T> Set<T> mapReference(Set<E> entities, Function<E, T> mapper) {
        if (entities == null || entities.isEmpty()) {
            return Set.of();
        }
        return entities.stream()
                .map(mapper)
                .collect(Collectors.toUnmodifiableSet());
    }
}
