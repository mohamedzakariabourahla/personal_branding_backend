package saas.personal_branding.api.infrastructure.provider.meta;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.platform.PlatformPublishCommand;
import saas.personal_branding.api.domain.platform.PlatformPublishResult;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.Platforms;

/**
 * Minimal Instagram publisher (content publish) using Meta Graph API.
 * Expects:
 * - connection metadata to include "instagramBusinessAccountId"
 * - mediaAssetIds[0] to contain an image URL (future: integrate with asset store)
 */
@Component
public class InstagramPublisher implements PlatformPublisher {

    private static final Logger log = LoggerFactory.getLogger(InstagramPublisher.class);
    private static final String IG_METADATA_KEY = "instagramBusinessAccountId";

    private final RestTemplate restTemplate;
    private final MetaOAuthProperties properties;

    public InstagramPublisher(@Qualifier("metaRestTemplate") RestTemplate restTemplate,
                              MetaOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public boolean supports(Platform platform) {
        if (platform == null) {
            return false;
        }
        String name = platform.getName();
        String code = platform.getCode();
        return (name != null && Platforms.INSTAGRAM.equalsIgnoreCase(name))
                || (code != null && Platforms.INSTAGRAM_CODE.equalsIgnoreCase(code));
    }

    @Override
    public PlatformPublishResult publish(PlatformPublishCommand command) {
        String accessToken = command.getAuthContext() != null ? command.getAuthContext().getAccessToken() : null;
        if (accessToken == null || accessToken.isBlank()) {
            return failure("MISSING_ACCESS_TOKEN", "Instagram access token is missing for this connection");
        }

        String igBusinessAccountId = resolveBusinessAccountId(command);
        if (igBusinessAccountId == null || igBusinessAccountId.isBlank()) {
            return failure("MISSING_IG_BUSINESS_ID", "Instagram business account id is missing in connection metadata");
        }

        if (command.getMediaAssetIds() == null || command.getMediaAssetIds().isEmpty()) {
            return failure("MISSING_MEDIA", "No media specified for publishing. Add an image URL to mediaAssetIds.");
        }

        try {
            String publishedId;
            Map<String, Object> raw = new HashMap<>();
            if (command.getMediaAssetIds().size() > 1) {
                var childIds = createCarouselItems(igBusinessAccountId, accessToken, command.getMediaAssetIds());
                String creationId = createCarouselContainer(igBusinessAccountId, accessToken, childIds, command.getCaption());
                if (!waitForReady(creationId, accessToken)) {
                    return failure("MEDIA_NOT_READY", "Media is not ready for publishing. Please try again shortly.");
                }
                publishedId = publishMedia(igBusinessAccountId, accessToken, creationId);
                raw.put("children", childIds);
                raw.put("creationId", creationId);
            } else {
                String imageUrl = resolveImageUrl(command);
                String creationId = createMediaContainer(igBusinessAccountId, accessToken, imageUrl, command.getCaption());
                if (!waitForReady(creationId, accessToken)) {
                    return failure("MEDIA_NOT_READY", "Media is not ready for publishing. Please try again shortly.");
                }
                publishedId = publishMedia(igBusinessAccountId, accessToken, creationId);
                raw.put("creationId", creationId);
            }

            raw.put("publishedId", publishedId);
            return PlatformPublishResult.builder()
                    .success(true)
                    .externalPostId(publishedId)
                    .publishedAt(Instant.now())
                    .rawResponse(raw)
                    .build();
        } catch (RestClientResponseException ex) {
            log.warn("Instagram publish failed status={} body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            return failure("HTTP_" + ex.getRawStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Instagram publish failed: {}", ex.getMessage());
            return failure("PUBLISH_ERROR", ex.getMessage());
        }
    }

    private String resolveImageUrl(PlatformPublishCommand command) {
        if (command.getMediaAssetIds() == null || command.getMediaAssetIds().isEmpty()) {
            return null;
        }
        String first = command.getMediaAssetIds().get(0);
        return normalizeUrl(first);
    }

    private String resolveBusinessAccountId(PlatformPublishCommand command) {
        if (command.getConnection() == null || command.getConnection().getAccountMetadata() == null) {
            return null;
        }
        var metadata = command.getConnection().getAccountMetadata();
        Object val = metadata.get(IG_METADATA_KEY);
        if (val == null) {
            val = metadata.get("instagramId"); // fallback for older connections
        }
        return val != null ? String.valueOf(val) : null;
    }

    private java.util.List<String> createCarouselItems(String igBusinessAccountId, String accessToken, java.util.List<String> mediaUrls) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String url : mediaUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            String child = createCarouselItem(igBusinessAccountId, accessToken, normalizeUrl(url));
            ids.add(child);
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No valid media URLs provided for carousel");
        }
        return ids;
    }

    private String createCarouselItem(String igBusinessAccountId, String accessToken, String imageUrl) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), igBusinessAccountId, "media")
                .toUriString();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image_url", imageUrl);
        body.add("is_carousel_item", "true");
        body.add("access_token", accessToken);

        var response = restTemplate.postForEntity(url, body, Map.class);
        Object id = Objects.requireNonNull(response.getBody()).get("id");
        if (id == null) {
            throw new IllegalStateException("Missing creation id from Instagram carousel item response");
        }
        return id.toString();
    }

    private String createCarouselContainer(String igBusinessAccountId, String accessToken, java.util.List<String> children, String caption) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), igBusinessAccountId, "media")
                .toUriString();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("media_type", "CAROUSEL");
        body.add("children", String.join(",", children));
        if (caption != null && !caption.isBlank()) {
            body.add("caption", caption);
        }
        body.add("access_token", accessToken);

        var response = restTemplate.postForEntity(url, body, Map.class);
        Object id = Objects.requireNonNull(response.getBody()).get("id");
        if (id == null) {
            throw new IllegalStateException("Missing creation id from Instagram carousel response");
        }
        return id.toString();
    }

    private String createMediaContainer(String igBusinessAccountId, String accessToken, String imageUrl, String caption) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), igBusinessAccountId, "media")
                .toUriString();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image_url", imageUrl);
        if (caption != null && !caption.isBlank()) {
            body.add("caption", caption);
        }
        body.add("access_token", accessToken);

        var response = restTemplate.postForEntity(url, body, Map.class);
        Object id = Objects.requireNonNull(response.getBody()).get("id");
        if (id == null) {
            throw new IllegalStateException("Missing creation id from Instagram media response");
        }
        return id.toString();
    }

    private String publishMedia(String igBusinessAccountId, String accessToken, String creationId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(properties.getApiVersion(), igBusinessAccountId, "media_publish")
                .toUriString();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("creation_id", creationId);
        body.add("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
        Object id = Objects.requireNonNull(response.getBody()).get("id");
        if (id == null) {
            throw new IllegalStateException("Missing published id from Instagram publish response");
        }
        return id.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean waitForReady(String creationId, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getGraphBaseUrl())
                .pathSegment(creationId)
                .queryParam("fields", "status_code,status")
                .queryParam("access_token", accessToken)
                .toUriString();

        for (int i = 0; i < 5; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Object statusCode = response.getBody() != null ? response.getBody().get("status_code") : null;
                if (statusCode != null) {
                    String code = statusCode.toString();
                    if ("FINISHED".equalsIgnoreCase(code)) {
                        return true;
                    }
                    if ("ERROR".equalsIgnoreCase(code) || "EXPIRED".equalsIgnoreCase(code)) {
                        return false;
                    }
                }
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception ex) {
                log.warn("Failed to poll media status for creationId {}: {}", creationId, ex.getMessage());
            }
        }
        return false;
    }

    private PlatformPublishResult failure(String code, String message) {
        return PlatformPublishResult.builder()
                .success(false)
                .errorCode(code)
                .errorMessage(message)
                .build();
    }

    private String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String url = raw.trim();
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }
}
