package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;

public interface JpaPlatformRepository extends JpaRepository<PlatformEntity, Long> {
}
