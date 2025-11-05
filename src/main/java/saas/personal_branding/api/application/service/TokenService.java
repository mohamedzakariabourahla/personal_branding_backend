package saas.personal_branding.api.application.service;

import saas.personal_branding.api.application.service.dto.TokenClaims;
import saas.personal_branding.api.domain.model.User;

public interface TokenService {
    String generateAccessToken(User user);
    TokenClaims parseAccessToken(String token);
}
