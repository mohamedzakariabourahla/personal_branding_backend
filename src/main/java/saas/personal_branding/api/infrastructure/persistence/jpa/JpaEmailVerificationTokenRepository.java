package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.personal_branding.api.infrastructure.persistence.entity.EmailVerificationTokenEntity;

import java.time.Instant;
import java.util.Optional;

public interface JpaEmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, Long> {

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from EmailVerificationTokenEntity t where t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update EmailVerificationTokenEntity t set t.usedAt = :usedAt where t.id = :tokenId and t.usedAt is null")
    int markUsed(@Param("tokenId") Long tokenId, @Param("usedAt") Instant usedAt);
}
