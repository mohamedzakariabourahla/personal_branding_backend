package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.ToneEntity;

public interface JpaToneRepository extends JpaRepository<ToneEntity, Long> {
}
