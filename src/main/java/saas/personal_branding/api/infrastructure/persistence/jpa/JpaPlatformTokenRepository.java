package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformTokenEntity;

import java.util.Optional;

public interface JpaPlatformTokenRepository extends JpaRepository<PlatformTokenEntity, Long> {

    Optional<PlatformTokenEntity> findByConnection_Id(Long connectionId);

    void deleteByConnection_Id(Long connectionId);
}
