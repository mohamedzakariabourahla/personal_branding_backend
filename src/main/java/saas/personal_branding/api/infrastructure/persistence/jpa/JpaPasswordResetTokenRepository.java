package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.personal_branding.api.infrastructure.persistence.entity.PasswordResetTokenEntity;

import java.time.Instant;
import java.util.Optional;

public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usedAt = :usedAt where t.id = :tokenId and t.usedAt is null")
    int markUsed(@Param("tokenId") Long tokenId, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
