package saas.personal_branding.api.publishing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.publishing.domain.PublishingAttempt;
import saas.personal_branding.api.publishing.domain.PublishingAttemptRepository;
import saas.personal_branding.api.publishing.infrastructure.persistence.entity.PublishingAttemptEntity;
import saas.personal_branding.api.publishing.infrastructure.persistence.jpa.JpaPublishingAttemptRepository;

@Repository
public class PublishingAttemptRepositoryAdapter implements PublishingAttemptRepository {

    private final JpaPublishingAttemptRepository jpaRepository;

    public PublishingAttemptRepositoryAdapter(JpaPublishingAttemptRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PublishingAttempt save(PublishingAttempt attempt) {
        PublishingAttemptEntity entity = toEntity(attempt);
        entity.setId(null);
        PublishingAttemptEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<PublishingAttempt> findByJobId(Long jobId, int limit) {
        int resolvedLimit = Math.max(1, limit);
        return jpaRepository.findByJobIdOrderByAttemptedAtDesc(jobId, PageRequest.of(0, resolvedLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private PublishingAttempt toDomain(PublishingAttemptEntity entity) {
        return PublishingAttempt.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .attemptedAt(entity.getAttemptedAt())
                .status(entity.getStatus())
                .error(entity.getError())
                .providerResponse(entity.getProviderResponse())
                .build();
    }

    private PublishingAttemptEntity toEntity(PublishingAttempt attempt) {
        PublishingAttemptEntity entity = new PublishingAttemptEntity();
        entity.setId(attempt.getId());
        entity.setJobId(attempt.getJobId());
        entity.setAttemptedAt(attempt.getAttemptedAt());
        entity.setStatus(attempt.getStatus());
        entity.setError(attempt.getError());
        entity.setProviderResponse(attempt.getProviderResponse());
        return entity;
    }
}
