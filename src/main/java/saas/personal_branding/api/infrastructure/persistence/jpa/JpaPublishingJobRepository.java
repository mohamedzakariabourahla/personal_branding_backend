package saas.personal_branding.api.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.personal_branding.api.infrastructure.persistence.entity.PublishingJobEntity;

public interface JpaPublishingJobRepository extends JpaRepository<PublishingJobEntity, Long> {

    @Query("SELECT j FROM PublishingJobEntity j WHERE j.status = saas.personal_branding.api.domain.scheduling.PublishingJobStatus.SCHEDULED AND j.scheduledAt <= :now ORDER BY j.scheduledAt ASC")
    List<PublishingJobEntity> findDue(@Param("now") Instant now, Pageable pageable);

    List<PublishingJobEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
