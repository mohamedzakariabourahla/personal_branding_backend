package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = {
            "person",
            "person.niches",
            "person.audiences",
            "person.tones",
            "person.platforms",
            "person.countries",
            "person.postingFrequencies"
    })
    Optional<UserEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {
            "person",
            "person.niches",
            "person.audiences",
            "person.tones",
            "person.platforms",
            "person.countries",
            "person.postingFrequencies"
    })
    Optional<UserEntity> findDetailedById(Long id);

    boolean existsByEmail(String email);
}
