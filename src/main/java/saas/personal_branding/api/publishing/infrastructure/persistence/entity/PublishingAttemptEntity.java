package saas.personal_branding.api.publishing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import saas.personal_branding.api.publishing.domain.PublishingAttemptStatus;

@Getter
@Setter
@Entity
@Table(name = "publishing_attempts")
public class PublishingAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PublishingAttemptStatus status;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "provider_response", columnDefinition = "text")
    private String providerResponse;
}
