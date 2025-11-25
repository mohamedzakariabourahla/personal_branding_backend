package saas.personal_branding.api.publishing.infrastructure.provider.tiktok;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.port.out.tiktok.TikTokApiClient;

@Component
public class DefaultTikTokApiClient implements TikTokApiClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultTikTokApiClient.class);

    private final RestTemplate restTemplate;
    private final TikTokOAuthProperties properties;

    public DefaultTikTokApiClient(RestTemplate restTemplate, TikTokOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public TikTokTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier) {
        String url = UriComponentsBuilder.fromHttpUrl("https://open-api.tiktok.com/oauth/token")
                .queryParam("client_key", properties.getClientKey())
                .queryParam("client_secret", properties.getClientSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .queryParam("redirect_uri", redirectUri)
                .toUriString();

        Map<String, Object> body = postForm(url);
        return toTokens(body);
    }

    @Override
    public TikTokTokens refreshTokens(String refreshToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://open-api.tiktok.com/oauth/refresh_token")
                .queryParam("client_key", properties.getClientKey())
                .queryParam("client_secret", properties.getClientSecret())
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .toUriString();

        Map<String, Object> body = postForm(url);
        return toTokens(body);
    }

    @Override
    public TikTokUser fetchUser(String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://open-api.tiktok.com/user/info/")
                .queryParam("fields", "open_id,display_name,avatar_url")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty TikTok user response");
        }
        Map<String, Object> data = getMap(body.get("data"));
        Map<String, Object> userMap = getMap(data.get("user"));
        String openId = getString(userMap.get("open_id"));
        String name = getString(userMap.get("display_name"));
        String avatar = getString(userMap.get("avatar_url"));
        return new TikTokUser(openId, name, avatar);
    }

    private Map<String, Object> postForm(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty TikTok response");
        }
        Object dataObj = body.getOrDefault("data", body);
        return getMap(dataObj);
    }

    private TikTokTokens toTokens(Map<String, Object> data) {
        String access = getString(data.get("access_token"));
        String refresh = getString(data.get("refresh_token"));
        Long expiresIn = getLong(data.get("expires_in"));
        Long refreshExpiresIn = getLong(data.get("refresh_expires_in"));
        String scope = getString(data.get("scope"));
        String tokenType = getString(data.get("token_type"));
        String openId = getString(data.get("open_id"));
        return new TikTokTokens(access, refresh, expiresIn, refreshExpiresIn, scope, tokenType, openId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object raw) {
        return raw instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private String getString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long getLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse long from {}", s);
            }
        }
        return null;
    }
}
