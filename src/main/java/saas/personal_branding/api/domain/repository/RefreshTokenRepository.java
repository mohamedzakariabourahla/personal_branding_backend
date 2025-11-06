package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    void revokeAllByUserId(Long userId);
    void revokeById(Long tokenId);
    Optional<RefreshToken> findActiveByTokenHash(String tokenHash);
    int revokeByUserIdAndDeviceId(Long userId, String deviceId);
    List<RefreshToken> findActiveByUserId(Long userId);
}
