package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "platform_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_platform_tokens_connection", columnNames = "connection_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PlatformTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false, unique = true)
    private PlatformConnectionEntity connection;

    @Column(name = "access_token_cipher")
    private String accessTokenCipher;

    @Column(name = "access_token_iv")
    private byte[] accessTokenIv;

    @Column(name = "refresh_token_cipher")
    private String refreshTokenCipher;

    @Column(name = "refresh_token_iv")
    private byte[] refreshTokenIv;

    @Column(name = "token_type", length = 64)
    private String tokenType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scopes", columnDefinition = "text[]")
    private String[] scopes;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "last_rotated_at")
    private Instant lastRotatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

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
