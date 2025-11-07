package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.PlatformOAuthState;

import java.time.Instant;
import java.util.Optional;

public interface PlatformOAuthStateRepository {

    PlatformOAuthState save(PlatformOAuthState state);

    Optional<PlatformOAuthState> findByState(String state);

    void deleteByState(String state);

    void purgeExpired(Instant cutoff);
}
