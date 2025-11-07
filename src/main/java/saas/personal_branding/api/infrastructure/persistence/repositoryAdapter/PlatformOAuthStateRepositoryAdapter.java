package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.domain.repository.PlatformOAuthStateRepository;
import saas.personal_branding.api.infrastructure.mapping.PlatformOAuthStateEntityMapper;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformOAuthStateEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPlatformOAuthStateRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPlatformRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.time.Instant;
import java.util.Optional;

@Repository
@Transactional
public class PlatformOAuthStateRepositoryAdapter implements PlatformOAuthStateRepository {

    private final JpaPlatformOAuthStateRepository stateRepository;
    private final JpaPlatformRepository platformRepository;
    private final JpaUserRepository userRepository;

    public PlatformOAuthStateRepositoryAdapter(JpaPlatformOAuthStateRepository stateRepository,
                                               JpaPlatformRepository platformRepository,
                                               JpaUserRepository userRepository) {
        this.stateRepository = stateRepository;
        this.platformRepository = platformRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PlatformOAuthState save(PlatformOAuthState state) {
        Platform platform = Optional.ofNullable(state.getPlatform())
                .orElseThrow(() -> new IllegalArgumentException("Platform is required"));
        PlatformEntity platformEntity = platformRepository.findById(platform.getId())
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platform.getId()));

        UserEntity userEntity = userRepository.findById(state.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + state.getUserId()));

        PlatformOAuthStateEntity entity = stateRepository.findById(state.getState())
                .orElse(new PlatformOAuthStateEntity());
        entity.setState(state.getState());
        entity.setPlatform(platformEntity);
        entity.setUser(userEntity);
        PlatformOAuthStateEntityMapper.copyToEntity(state, entity);

        PlatformOAuthStateEntity saved = stateRepository.save(entity);
        return PlatformOAuthStateEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformOAuthState> findByState(String state) {
        return stateRepository.findById(state)
                .map(PlatformOAuthStateEntityMapper::toDomain);
    }

    @Override
    public void deleteByState(String state) {
        stateRepository.deleteById(state);
    }

    @Override
    public void purgeExpired(Instant cutoff) {
        stateRepository.deleteByExpiresAtBefore(cutoff);
    }
}
