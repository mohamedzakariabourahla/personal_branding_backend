package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformToken;
import saas.personal_branding.api.domain.repository.PlatformCredentialRepository;
import saas.personal_branding.api.infrastructure.mapping.PlatformConnectionEntityMapper;
import saas.personal_branding.api.infrastructure.mapping.PlatformTokenEntityMapper;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformConnectionEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPlatformConnectionRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPlatformRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPlatformTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class PlatformCredentialRepositoryAdapter implements PlatformCredentialRepository {

    private final JpaPlatformConnectionRepository connectionRepository;
    private final JpaPlatformTokenRepository tokenRepository;
    private final JpaPlatformRepository platformRepository;
    private final JpaUserRepository userRepository;

    public PlatformCredentialRepositoryAdapter(JpaPlatformConnectionRepository connectionRepository,
                                               JpaPlatformTokenRepository tokenRepository,
                                               JpaPlatformRepository platformRepository,
                                               JpaUserRepository userRepository) {
        this.connectionRepository = connectionRepository;
        this.tokenRepository = tokenRepository;
        this.platformRepository = platformRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PlatformConnection saveConnection(PlatformConnection connection) {
        UserEntity userEntity = userRepository.findById(connection.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + connection.getUserId()));

        Platform platform = Optional.ofNullable(connection.getPlatform())
                .orElseThrow(() -> new IllegalArgumentException("Platform is required"));
        PlatformEntity platformEntity = platformRepository.findById(platform.getId())
                .orElseThrow(() -> new IllegalArgumentException("Platform not found: " + platform.getId()));

        PlatformConnectionEntity entity = connection.getId() != null
                ? connectionRepository.findById(connection.getId())
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connection.getId()))
                : new PlatformConnectionEntity();

        entity.setUser(userEntity);
        entity.setPlatform(platformEntity);
        PlatformConnectionEntityMapper.copyToEntity(connection, entity);

        PlatformConnectionEntity saved = connectionRepository.save(entity);
        return PlatformConnectionEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformConnection> findConnectionById(Long connectionId) {
        return connectionRepository.findById(connectionId)
                .map(PlatformConnectionEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformConnection> findConnectionsByUserId(Long userId) {
        return connectionRepository.findAllByUser_Id(userId).stream()
                .map(PlatformConnectionEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformConnection> findConnection(Long userId, Long platformId, String externalAccountId) {
        return connectionRepository.findByUser_IdAndPlatform_IdAndExternalAccountId(userId, platformId, externalAccountId)
                .map(PlatformConnectionEntityMapper::toDomain);
    }

    @Override
    public void deleteConnection(Long connectionId) {
        tokenRepository.deleteByConnection_Id(connectionId);
        connectionRepository.deleteById(connectionId);
    }

    @Override
    public PlatformToken saveToken(PlatformToken token) {
        if (token.getConnectionId() == null) {
            throw new IllegalArgumentException("Connection id is required for tokens");
        }
        PlatformConnectionEntity connectionEntity = connectionRepository.findById(token.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + token.getConnectionId()));

        PlatformTokenEntity entity = tokenRepository.findByConnection_Id(token.getConnectionId())
                .orElse(new PlatformTokenEntity());
        entity.setConnection(connectionEntity);
        PlatformTokenEntityMapper.copyToEntity(token, entity);

        PlatformTokenEntity saved = tokenRepository.save(entity);
        return PlatformTokenEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformToken> findTokenByConnectionId(Long connectionId) {
        return tokenRepository.findByConnection_Id(connectionId)
                .map(PlatformTokenEntityMapper::toDomain);
    }

    @Override
    public void deleteTokenByConnectionId(Long connectionId) {
        tokenRepository.deleteByConnection_Id(connectionId);
    }
}
