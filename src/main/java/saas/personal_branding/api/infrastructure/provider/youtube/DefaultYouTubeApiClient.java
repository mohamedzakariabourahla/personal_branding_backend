package saas.personal_branding.api.infrastructure.provider.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import saas.personal_branding.api.application.port.out.youtube.YouTubeApiClient;

import java.util.List;

@Component
public class DefaultYouTubeApiClient implements YouTubeApiClient {

    private final RestTemplate youTubeRestTemplate;
    private final YouTubeOAuthProperties properties;

    public DefaultYouTubeApiClient(@Qualifier("youTubeRestTemplate") RestTemplate youTubeRestTemplate,
                                   YouTubeOAuthProperties properties) {
        this.youTubeRestTemplate = youTubeRestTemplate;
        this.properties = properties;
    }

    @Override
    public YouTubeTokens exchangeCodeForTokens(String code, String redirectUri, String codeVerifier) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);
        params.add("code_verifier", codeVerifier);
        return postForTokens(params);
    }

    @Override
    public YouTubeTokens refreshTokens(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        return postForTokens(params);
    }

    @Override
    public List<YouTubeChannel> fetchChannels(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        String url = properties.getApiBaseUrl() + "/youtube/v3/channels?part=snippet&mine=true";
        var response = youTubeRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), YouTubeChannelsResponse.class);
        YouTubeChannelsResponse body = response.getBody();
        if (body == null || body.items == null) {
            throw new IllegalStateException("YouTube channels response missing data");
        }
        return body.items.stream()
                .map(item -> {
                    String handle = item.snippet != null ? firstNonNull(item.snippet.customUrl, item.snippet.title) : null;
                    String thumbnailUrl = item.snippet != null && item.snippet.thumbnails != null
                            ? item.snippet.thumbnails.bestAvailableUrl()
                            : null;
                    String title = item.snippet != null ? item.snippet.title : item.id;
                    return new YouTubeChannel(item.id, title, handle, thumbnailUrl);
                })
                .toList();
    }

    private YouTubeTokens postForTokens(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var response = youTubeRestTemplate.postForEntity(
                properties.getTokenUrl(),
                new HttpEntity<>(params, headers),
                YouTubeTokenResponse.class
        );
        YouTubeTokenResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("YouTube token response missing data");
        }
        return new YouTubeTokens(
                body.access_token,
                body.refresh_token,
                body.expires_in,
                body.scope,
                body.token_type
        );
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record YouTubeTokenResponse(String access_token,
                                        String refresh_token,
                                        Long expires_in,
                                        String scope,
                                        String token_type) {
    }

    private record YouTubeChannelsResponse(List<Item> items) {
        private record Item(String id, Snippet snippet) {
        }

        private record Snippet(String title, String customUrl, Thumbnails thumbnails) {
        }

        private record Thumbnails(@JsonProperty("default") Thumbnail _default,
                                  Thumbnail medium,
                                  Thumbnail high) {
            private String bestAvailableUrl() {
                if (high != null && high.url != null) {
                    return high.url;
                }
                if (medium != null && medium.url != null) {
                    return medium.url;
                }
                if (_default != null) {
                    return _default.url;
                }
                return null;
            }
        }

        private record Thumbnail(String url) {
        }
    }
}
