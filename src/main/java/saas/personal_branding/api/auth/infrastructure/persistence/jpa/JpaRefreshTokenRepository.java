package saas.personal_branding.api.auth.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.id = :tokenId")
    void revokeById(@Param("tokenId") Long tokenId);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.user.id = :userId and t.deviceId = :deviceId")
    int revokeByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Query("select t from RefreshTokenEntity t where t.user.id = :userId and t.revoked = false and t.expiresAt > :now order by t.lastUsedAt desc")
    List<RefreshTokenEntity> findActiveByUserIdOrderByLastUsedAtDesc(@Param("userId") Long userId, @Param("now") Instant now);
}
