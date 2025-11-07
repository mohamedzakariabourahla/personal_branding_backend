package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;

import java.util.Optional;

public interface JpaPlatformRepository extends JpaRepository<PlatformEntity, Long> {
    Optional<PlatformEntity> findByNameIgnoreCase(String name);
}
