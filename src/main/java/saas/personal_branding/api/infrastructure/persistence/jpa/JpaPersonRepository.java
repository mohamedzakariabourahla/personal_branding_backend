package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PersonEntity;

import java.util.Optional;

public interface JpaPersonRepository extends JpaRepository<PersonEntity, Long> {

    @EntityGraph(attributePaths = {
            "niches",
            "audiences",
            "tones",
            "platforms",
            "countries",
            "postingFrequencies"
    })
    Optional<PersonEntity> findByUserId(Long userId);
}
