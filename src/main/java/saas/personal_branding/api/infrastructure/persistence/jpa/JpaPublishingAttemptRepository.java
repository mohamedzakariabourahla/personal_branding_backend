package saas.personal_branding.api.infrastructure.persistence.jpa;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PublishingAttemptEntity;

public interface JpaPublishingAttemptRepository extends JpaRepository<PublishingAttemptEntity, Long> {

    List<PublishingAttemptEntity> findByJobIdOrderByAttemptedAtDesc(Long jobId, Pageable pageable);
}
