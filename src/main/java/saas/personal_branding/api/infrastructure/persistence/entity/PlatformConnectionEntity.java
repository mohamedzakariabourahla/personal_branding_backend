package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import saas.personal_branding.api.domain.model.PlatformConnectionStatus;

import java.time.Instant;

@Entity
@Table(name = "platform_connections",
        uniqueConstraints = @UniqueConstraint(name = "uk_platform_connection_account",
                columnNames = {"user_id", "platform_id", "external_account_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PlatformConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private PlatformEntity platform;

    @Column(name = "external_account_id", nullable = false, length = 128)
    private String externalAccountId;

    @Column(name = "external_username", length = 255)
    private String externalUsername;

    @Column(name = "external_display_name", length = 255)
    private String externalDisplayName;

    @Column(name = "account_metadata", columnDefinition = "jsonb")
    private String accountMetadata;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PlatformConnectionStatus status = PlatformConnectionStatus.CONNECTED;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
