package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
