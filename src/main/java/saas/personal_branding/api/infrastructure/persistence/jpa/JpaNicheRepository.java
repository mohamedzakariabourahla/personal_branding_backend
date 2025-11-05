package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.NicheEntity;

public interface JpaNicheRepository extends JpaRepository<NicheEntity, Long> {
}
