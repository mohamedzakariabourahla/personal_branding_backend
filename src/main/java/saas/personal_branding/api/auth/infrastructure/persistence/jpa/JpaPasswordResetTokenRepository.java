package saas.personal_branding.api.auth.infrastructure.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;

@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usedAt = :usedAt where t.id = :tokenId")
    void markUsed(@Param("tokenId") Long tokenId, @Param("usedAt") java.time.Instant usedAt);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
