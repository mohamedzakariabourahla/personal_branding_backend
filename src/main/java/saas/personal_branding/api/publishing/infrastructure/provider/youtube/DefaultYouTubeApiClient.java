package saas.personal_branding.api.publishing.infrastructure.provider.youtube;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.port.out.youtube.YouTubeApiClient;

@Component
public class DefaultYouTubeApiClient implements YouTubeApiClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultYouTubeApiClient.class);

    private final RestTemplate restTemplate;
    private final YouTubeOAuthProperties properties;

    public DefaultYouTubeApiClient(RestTemplate restTemplate, YouTubeOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public YouTubeTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier) {
        String url = "https://oauth2.googleapis.com/token";
        StringBuilder body = new StringBuilder();
        body.append("code=").append(code);
        body.append("&client_id=").append(properties.getClientId());
        body.append("&client_secret=").append(properties.getClientSecret());
        body.append("&redirect_uri=").append(redirectUri);
        body.append("&grant_type=authorization_code");
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            body.append("&code_verifier=").append(codeVerifier);
        }

        Map<String, Object> payload = postForm(url, body.toString());
        return toTokens(payload);
    }

    @Override
    public YouTubeTokens refreshTokens(String refreshToken) {
        String url = "https://oauth2.googleapis.com/token";
        String body = "client_id=" + properties.getClientId()
                + "&client_secret=" + properties.getClientSecret()
                + "&refresh_token=" + refreshToken
                + "&grant_type=refresh_token";
        Map<String, Object> payload = postForm(url, body);
        return toTokens(payload);
    }

    @Override
    public List<YouTubeChannel> fetchChannels(String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/channels")
                .queryParam("part", "id,snippet")
                .queryParam("mine", "true")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = response.getBody();
        List<YouTubeChannel> channels = new ArrayList<>();
        if (body == null) {
            return channels;
        }
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object item : items) {
                Map<String, Object> itemMap = castMap(item);
                Map<String, Object> snippet = castMap(itemMap.get("snippet"));
                String id = getString(itemMap.get("id"));
                String title = getString(snippet.get("title"));
                String handle = getString(snippet.get("customUrl"));
                String thumb = null;
                Map<String, Object> thumbs = castMap(snippet.get("thumbnails"));
                Map<String, Object> defaultThumb = castMap(thumbs.get("default"));
                if (!defaultThumb.isEmpty()) {
                    thumb = getString(defaultThumb.get("url"));
                }
                channels.add(new YouTubeChannel(id, title, handle, thumb));
            }
        }
        return channels;
    }

    private Map<String, Object> postForm(String url, String formBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(formBody, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty YouTube token response");
        }
        return body;
    }

    private YouTubeTokens toTokens(Map<String, Object> payload) {
        String access = getString(payload.get("access_token"));
        String refresh = getString(payload.get("refresh_token"));
        Long expires = getLong(payload.get("expires_in"));
        String scope = getString(payload.get("scope"));
        String tokenType = getString(payload.getOrDefault("token_type", "Bearer"));
        return new YouTubeTokens(access, refresh, expires, scope, tokenType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object raw) {
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
