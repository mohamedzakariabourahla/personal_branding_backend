package saas.personal_branding.api.publishing.infrastructure.persistence;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.publishing.domain.PublishingJob;
import saas.personal_branding.api.publishing.domain.PublishingJobRepository;
import saas.personal_branding.api.publishing.infrastructure.persistence.entity.PublishingJobEntity;
import saas.personal_branding.api.publishing.infrastructure.persistence.jpa.JpaPublishingJobRepository;

@Repository
public class PublishingJobRepositoryAdapter implements PublishingJobRepository {

    private final JpaPublishingJobRepository jpaRepository;

    public PublishingJobRepositoryAdapter(JpaPublishingJobRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PublishingJob save(PublishingJob job) {
        PublishingJobEntity entity = toEntity(job);
        entity.setId(null);
        PublishingJobEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PublishingJob> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PublishingJob> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PublishingJob update(PublishingJob job) {
        if (job.getId() == null) {
            throw new IllegalArgumentException("Job id is required to update");
        }
        PublishingJobEntity saved = jpaRepository.save(toEntity(job));
        return toDomain(saved);
    }

    @Override
    public List<PublishingJob> findDue(Instant now, int max) {
        int limit = Math.max(1, max);
        return jpaRepository.findDue(now, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PublishingJob> findRecentByUserId(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int resolvedLimit = Math.max(1, limit);
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, resolvedLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private PublishingJobEntity toEntity(PublishingJob job) {
        PublishingJobEntity entity = new PublishingJobEntity();
        entity.setId(job.getId());
        entity.setUserId(job.getUserId());
        entity.setPlatformId(job.getPlatformId());
        entity.setConnectionId(job.getConnectionId());
        entity.setMediaAssetIds(join(job.getMediaAssetIds()));
        entity.setCaption(job.getCaption());
        entity.setScheduledAt(job.getScheduledAt());
        entity.setCreatedAt(job.getCreatedAt());
        entity.setLastTriedAt(job.getLastTriedAt());
        entity.setCompletedAt(job.getCompletedAt());
        entity.setAttemptCount(job.getAttemptCount());
        entity.setStatus(job.getStatus());
        entity.setFailureReason(job.getFailureReason());
        entity.setExternalPostId(job.getExternalPostId());
        return entity;
    }

    private PublishingJob toDomain(PublishingJobEntity entity) {
        return PublishingJob.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .platformId(entity.getPlatformId())
                .connectionId(entity.getConnectionId())
                .mediaAssetIds(split(entity.getMediaAssetIds()))
                .caption(entity.getCaption())
                .scheduledAt(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt())
                .lastTriedAt(entity.getLastTriedAt())
                .completedAt(entity.getCompletedAt())
                .attemptCount(entity.getAttemptCount())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .externalPostId(entity.getExternalPostId())
                .build();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("||", values);
    }

    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
