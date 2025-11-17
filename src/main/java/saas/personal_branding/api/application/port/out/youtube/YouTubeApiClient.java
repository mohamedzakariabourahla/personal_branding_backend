package saas.personal_branding.api.application.port.out.youtube;

import java.util.List;

public interface YouTubeApiClient {

    YouTubeTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier);

    YouTubeTokens refreshTokens(String refreshToken);

    List<YouTubeChannel> fetchChannels(String accessToken);

    record YouTubeTokens(String accessToken,
                         String refreshToken,
                         Long expiresInSeconds,
                         String scope,
                         String tokenType) {
    }

    record YouTubeChannel(String id,
                          String title,
                          String handle,
                          String thumbnailUrl) {
    }
}
