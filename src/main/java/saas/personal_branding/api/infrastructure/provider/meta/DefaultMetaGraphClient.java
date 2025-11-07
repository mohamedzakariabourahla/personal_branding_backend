package saas.personal_branding.api.infrastructure.provider.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
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
        String url = properties.getGraphBaseUrl() + "/" + properties.getApiVersion()
                + "/me/accounts?fields=id,name,access_token,instagram_business_account{id,username,name}&access_token=" + accessToken;
        ResponseEntity<MetaAccountsResponse> response = restTemplate.getForEntity(url, MetaAccountsResponse.class);
        MetaAccountsResponse body = response.getBody();
        if (body == null || body.data == null) {
            return List.of();
        }
        return Arrays.stream(body.data)
                .map(page -> new MetaPage(
                        page.id,
                        page.name,
                        page.access_token,
                        page.instagram_business_account == null ? null :
                                new MetaInstagramAccount(
                                        page.instagram_business_account.id,
                                        page.instagram_business_account.username,
                                        page.instagram_business_account.name
                                )
                ))
                .toList();
    }

    private MetaAccessToken getAccessToken(String url, MultiValueMap<String, String> params) {
        String fullUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url)
                .queryParams(params)
                .build(true)
                .toUriString();
        ResponseEntity<MetaAccessTokenResponse> response = restTemplate.getForEntity(fullUrl, MetaAccessTokenResponse.class);
        MetaAccessTokenResponse body = response.getBody();
        if (body == null || body.access_token == null) {
            throw new IllegalStateException("Meta response missing access token");
        }
        return new MetaAccessToken(body.access_token, body.token_type, body.expires_in);
    }

    private record MetaAccessTokenResponse(String access_token, String token_type, Long expires_in) {
    }

    private record MetaAccountsResponse(MetaPageResponse[] data, Map<String, Object> paging) {
    }

    private record MetaPageResponse(String id, String name, String access_token, MetaInstagramAccountResponse instagram_business_account) {
    }

    private record MetaInstagramAccountResponse(String id, String username, String name) {
    }
}
