package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformOAuthStateEntity;

import java.time.Instant;

public interface JpaPlatformOAuthStateRepository extends JpaRepository<PlatformOAuthStateEntity, String> {

    void deleteByExpiresAtBefore(Instant cutoff);
}
