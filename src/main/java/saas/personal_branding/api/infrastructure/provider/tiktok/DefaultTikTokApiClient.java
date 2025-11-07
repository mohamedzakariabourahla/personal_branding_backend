package saas.personal_branding.api.infrastructure.provider.tiktok;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import saas.personal_branding.api.application.port.out.tiktok.TikTokApiClient;

import java.util.List;

@Component
public class DefaultTikTokApiClient implements TikTokApiClient {

    private final RestTemplate tikTokRestTemplate;
    private final TikTokOAuthProperties properties;

    public DefaultTikTokApiClient(@Qualifier("tikTokRestTemplate") RestTemplate tikTokRestTemplate,
                                  TikTokOAuthProperties properties) {
        this.tikTokRestTemplate = tikTokRestTemplate;
        this.properties = properties;
    }

    @Override
    public TikTokTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_key", properties.getClientKey());
        params.add("client_secret", properties.getClientSecret());
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);
        params.add("code_verifier", codeVerifier);
        return postForTokens("/v2/oauth/token/", params);
    }

    @Override
    public TikTokTokens refreshTokens(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_key", properties.getClientKey());
        params.add("client_secret", properties.getClientSecret());
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        return postForTokens("/v2/oauth/token/", params);
    }

    @Override
    public TikTokUser fetchUser(String accessToken) {
        String url = properties.getApiBaseUrl() + "/v2/user/info/";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = new TikTokUserInfoRequest(List.of("open_id", "display_name", "avatar_url"));
        var response = tikTokRestTemplate.postForEntity(url, new HttpEntity<>(body, headers), TikTokUserInfoResponse.class);
        TikTokUserInfoResponse respBody = response.getBody();
        if (respBody == null || respBody.data == null || respBody.data.user == null) {
            throw new IllegalStateException("TikTok user info response missing data");
        }
        var user = respBody.data.user;
        return new TikTokUser(user.open_id, user.display_name, user.avatar_url);
    }

    private TikTokTokens postForTokens(String path, MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var response = tikTokRestTemplate.postForEntity(
                properties.getApiBaseUrl() + path,
                new HttpEntity<>(params, headers),
                TikTokTokenResponse.class);
        TikTokTokenResponse body = response.getBody();
        if (body == null || body.data == null) {
            throw new IllegalStateException("TikTok token response missing data");
        }
        TikTokTokenResponse.TokenData data = body.data;
        return new TikTokTokens(
                data.access_token,
                data.refresh_token,
                data.expires_in,
                data.refresh_expires_in,
                data.scope,
                data.token_type,
                data.open_id
        );
    }

    private record TikTokTokenResponse(TokenData data) {
        private record TokenData(String access_token,
                                 String refresh_token,
                                 Long expires_in,
                                 Long refresh_expires_in,
                                 String scope,
                                 String token_type,
                                 String open_id) {
        }
    }

    private record TikTokUserInfoRequest(List<String> fields) {
    }

    private record TikTokUserInfoResponse(UserData data) {
        private record UserData(User user) {
        }

        private record User(String open_id, String display_name, String avatar_url) {
        }
    }
}
