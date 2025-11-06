package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.UserRepository;
import saas.personal_branding.api.infrastructure.mapping.UserEntityMapper;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.util.List;
import java.util.Optional;

import static saas.personal_branding.api.domain.util.EmailNormalizer.normalize;
import static saas.personal_branding.api.infrastructure.mapping.UserEntityMapper.toDomain;
import static saas.personal_branding.api.infrastructure.mapping.UserEntityMapper.updateEntity;

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

        updateEntity(user, entity);

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
        String normalized = normalize(email);
        if (normalized == null) {
            return Optional.empty();
        }
        return jpaUserRepository.findByEmail(normalized)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null) {
            return false;
        }
        return jpaUserRepository.existsByEmail(normalized);
    }

    @Override
    public void updateOnboardingStatus(Long userId, OnboardingStatus status) {
        UserEntity entity = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        entity.setOnboardingStatus(status);
        UserEntity saved = jpaUserRepository.save(entity);

        jpaUserRepository.findDetailedById(saved.getId())
                .map(this::toDomain)
                .orElseGet(() -> toDomain(saved));
    }

    private User toDomain(UserEntity entity) {
        return UserEntityMapper.toDomain(entity);
    }
}
