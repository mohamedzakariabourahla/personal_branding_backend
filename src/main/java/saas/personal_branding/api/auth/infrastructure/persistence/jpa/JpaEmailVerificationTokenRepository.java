package saas.personal_branding.api.auth.infrastructure.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.EmailVerificationTokenEntity;

@Repository
public interface JpaEmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, Long> {

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from EmailVerificationTokenEntity t where t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update EmailVerificationTokenEntity t set t.usedAt = :usedAt where t.id = :tokenId")
    void markUsed(@Param("tokenId") Long tokenId, @Param("usedAt") java.time.Instant usedAt);
}
