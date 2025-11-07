package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.model.PlatformToken;

import java.util.List;
import java.util.Optional;

public interface PlatformCredentialRepository {

    PlatformConnection saveConnection(PlatformConnection connection);

    Optional<PlatformConnection> findConnectionById(Long connectionId);

    List<PlatformConnection> findConnectionsByUserId(Long userId);

    Optional<PlatformConnection> findConnection(Long userId, Long platformId, String externalAccountId);

    void deleteConnection(Long connectionId);

    PlatformToken saveToken(PlatformToken token);

    Optional<PlatformToken> findTokenByConnectionId(Long connectionId);

    void deleteTokenByConnectionId(Long connectionId);
}
