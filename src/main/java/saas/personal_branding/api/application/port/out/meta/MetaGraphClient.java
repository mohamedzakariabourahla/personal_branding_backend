package saas.personal_branding.api.application.port.out.meta;

import java.util.List;

public interface MetaGraphClient {

    MetaAccessToken exchangeCodeForUserToken(String code, String redirectUri);

    MetaAccessToken exchangeForLongLivedUserToken(String shortLivedToken);

    List<MetaPage> fetchPages(String accessToken);

    record MetaAccessToken(String accessToken, String tokenType, Long expiresInSeconds) {
    }

    record MetaPage(String id,
                    String name,
                    String accessToken,
                    MetaInstagramAccount instagramAccount) {
    }

    record MetaInstagramAccount(String id, String username, String name) {
    }
}
