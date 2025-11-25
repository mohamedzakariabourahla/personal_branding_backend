package saas.personal_branding.api.publishing.infrastructure.provider.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.port.out.meta.MetaGraphClient;

@Component
public class DefaultMetaGraphClient implements MetaGraphClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMetaGraphClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MetaOAuthProperties properties;

    public DefaultMetaGraphClient(RestTemplate restTemplate, ObjectMapper objectMapper, MetaOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public MetaAccessToken exchangeCodeForUserToken(String code, String redirectUri) {
        String url = String.format("https://graph.facebook.com/%s/oauth/access_token?client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                properties.getApiVersion(),
                properties.getClientId(),
                properties.getClientSecret(),
                redirectUri,
                code);
        Map<String, Object> body = get(url);
        String accessToken = (String) body.get("access_token");
        String tokenType = (String) body.getOrDefault("token_type", "Bearer");
        Long expiresIn = asLong(body.get("expires_in"));
        if (accessToken == null) {
            throw new IllegalStateException("Meta did not return access_token");
        }
        return new MetaAccessToken(accessToken, tokenType, expiresIn);
    }

    @Override
    public MetaAccessToken exchangeForLongLivedUserToken(String shortLivedToken) {
        String url = String.format("https://graph.facebook.com/%s/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
                properties.getApiVersion(),
                properties.getClientId(),
                properties.getClientSecret(),
                shortLivedToken);
        Map<String, Object> body = get(url);
        String accessToken = (String) body.get("access_token");
        String tokenType = (String) body.getOrDefault("token_type", "Bearer");
        Long expiresIn = asLong(body.get("expires_in"));
        if (accessToken == null) {
            throw new IllegalStateException("Meta did not return long-lived access_token");
        }
        return new MetaAccessToken(accessToken, tokenType, expiresIn);
    }

    @Override
    public List<MetaPage> fetchPages(String accessToken) {
        String url = String.format("https://graph.facebook.com/%s/me/accounts", properties.getApiVersion());
        // Pre-encode the sub-field braces to avoid URI validation issues.
        String fields = "name,access_token,instagram_business_account%7Bname,username%7D";
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("fields", fields)
                .queryParam("access_token", accessToken)
                .build(true)
                .toUri();

        Map<String, Object> body = restTemplate.exchange(RequestEntity.get(uri).build(), Map.class).getBody();
        Object dataObj = body.get("data");
        if (!(dataObj instanceof List<?> dataList)) {
            return List.of();
        }
        return dataList.stream()
                .map(this::toPage)
                .collect(Collectors.toList());
    }

    private MetaPage toPage(Object raw) {
        try {
            Map<String, Object> map = objectMapper.convertValue(raw, Map.class);
            Map<String, Object> ig = map.get("instagram_business_account") instanceof Map<?, ?> m ? (Map<String, Object>) m : Collections.emptyMap();
            MetaInstagramAccount igAccount = ig.isEmpty() ? null :
                    new MetaInstagramAccount(
                            (String) ig.get("id"),
                            (String) ig.get("username"),
                            (String) ig.get("name")
                    );
            return new MetaPage(
                    (String) map.get("id"),
                    (String) map.get("name"),
                    (String) map.get("access_token"),
                    igAccount
            );
        } catch (Exception ex) {
            log.warn("Failed to parse Meta page entry: {}", raw, ex);
            return null;
        }
    }

    private Map<String, Object> get(String url) {
        URI uri = UriComponentsBuilder.fromHttpUrl(url).build(true).toUri();
        return restTemplate.exchange(RequestEntity.get(uri).build(), Map.class).getBody();
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
