package saas.personal_branding.api.application.port.out.tiktok;

public interface TikTokApiClient {

    TikTokTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier);

    TikTokTokens refreshTokens(String refreshToken);

    TikTokUser fetchUser(String accessToken);

    record TikTokTokens(String accessToken,
                        String refreshToken,
                        Long expiresInSeconds,
                        Long refreshExpiresInSeconds,
                        String scope,
                        String tokenType,
                        String openId) {
    }

    record TikTokUser(String openId, String displayName, String avatarUrl) {
    }
}
