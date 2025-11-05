package saas.personal_branding.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PostingFrequencyEntity;

public interface JpaPostingFrequencyRepository extends JpaRepository<PostingFrequencyEntity, Long> {
}
