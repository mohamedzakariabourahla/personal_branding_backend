package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformConnectionEntity;

import java.util.List;
import java.util.Optional;

public interface JpaPlatformConnectionRepository extends JpaRepository<PlatformConnectionEntity, Long> {

    List<PlatformConnectionEntity> findAllByUser_Id(Long userId);

    Optional<PlatformConnectionEntity> findByUser_IdAndPlatform_IdAndExternalAccountId(Long userId, Long platformId, String externalAccountId);
}
