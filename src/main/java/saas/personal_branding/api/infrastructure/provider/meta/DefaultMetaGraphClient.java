package saas.personal_branding.api.infrastructure.provider.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.port.out.meta.MetaGraphClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class DefaultMetaGraphClient implements MetaGraphClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMetaGraphClient.class);

    private final RestTemplate restTemplate;
    private final MetaOAuthProperties properties;

    public DefaultMetaGraphClient(@Qualifier("metaRestTemplate") RestTemplate restTemplate,
                                  MetaOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public MetaAccessToken exchangeCodeForUserToken(String code, String redirectUri) {
        String url = properties.getGraphBaseUrl() + "/" + properties.getApiVersion() + "/oauth/access_token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        return getAccessToken(url, params);
    }

    @Override
    public MetaAccessToken exchangeForLongLivedUserToken(String shortLivedToken) {
        String url = properties.getGraphBaseUrl() + "/" + properties.getApiVersion() + "/oauth/access_token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "fb_exchange_token");
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("fb_exchange_token", shortLivedToken);
        return getAccessToken(url, params);
    }

    @Override
    public List<MetaPage> fetchPages(String accessToken) {
        String fields = "id,name,access_token,instagram_business_account";
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), "me", "accounts")
                .queryParam("fields", fields)
                .queryParam("access_token", "{token}")
                .buildAndExpand(accessToken)
                .encode()
                .toUriString();
        MetaAccountsResponse body = getForEntity(url, MetaAccountsResponse.class, "fetch pages");
        if (body == null || body.data == null) {
            return List.of();
        }
        return Arrays.stream(body.data)
                .map(page -> new MetaPage(
                        page.id,
                        page.name,
                        page.access_token,
                        resolveInstagramAccount(page, accessToken)
                ))
                .toList();
    }

    private MetaInstagramAccount resolveInstagramAccount(MetaPageResponse page, String fallbackToken) {
        if (page.instagram_business_account == null || page.instagram_business_account.id == null) {
            return null;
        }
        String igId = page.instagram_business_account.id;
        String fields = "id,username,name";
        String tokenToUse = page.access_token != null ? page.access_token : fallbackToken;
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), igId)
                .queryParam("fields", fields)
                .queryParam("access_token", "{token}")
                .buildAndExpand(tokenToUse)
                .encode()
                .toUriString();
        MetaInstagramAccountResponse response = getForEntity(url, MetaInstagramAccountResponse.class, "fetch instagram account");
        if (response == null || response.id == null) {
            return null;
        }
        return new MetaInstagramAccount(response.id, response.username, response.name);
    }

    private MetaAccessToken getAccessToken(String url, MultiValueMap<String, String> params) {
        String fullUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url)
                .queryParams(params)
                .build(true)
                .toUriString();
        MetaAccessTokenResponse body = getForEntity(fullUrl, MetaAccessTokenResponse.class, "exchange tokens");
        if (body == null || body.access_token == null) {
            throw new PlatformException.ProviderCommunicationException("Meta response missing access token");
        }
        return new MetaAccessToken(body.access_token, body.token_type, body.expires_in);
    }

    private <T> T getForEntity(String url, Class<T> responseType, String context) {
        try {
            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            log.error("Meta API {} failed: status={} body={}", context, ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new PlatformException.ProviderCommunicationException("Meta API " + context + " failed: " + extractErrorMessage(ex));
        } catch (RestClientException ex) {
            log.error("Meta API {} failed: {}", context, ex.getMessage(), ex);
            throw new PlatformException.ProviderCommunicationException("Meta API " + context + " failed. Please try again later.");
        }
    }

    private String extractErrorMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "HTTP " + ex.getRawStatusCode();
        }
        return body;
    }

    private record MetaAccessTokenResponse(String access_token, String token_type, Long expires_in) {
    }

    private record MetaAccountsResponse(MetaPageResponse[] data, Map<String, Object> paging) {
    }

    private record MetaPageResponse(String id, String name, String access_token, MetaInstagramAccountStub instagram_business_account) {
    }

    private record MetaInstagramAccountStub(String id) {
    }

    private record MetaInstagramAccountResponse(String id, String username, String name) {
    }
}
