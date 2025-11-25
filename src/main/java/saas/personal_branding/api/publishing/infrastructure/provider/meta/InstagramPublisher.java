package saas.personal_branding.api.publishing.infrastructure.provider.meta;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.domain.platform.PlatformAuthContext;
import saas.personal_branding.api.domain.platform.PlatformPublishCommand;
import saas.personal_branding.api.domain.platform.PlatformPublishResult;
import saas.personal_branding.api.domain.platform.PlatformPublisher;
import saas.personal_branding.api.domain.platform.Platforms;

@Component
public class InstagramPublisher implements PlatformPublisher {

    private static final Logger log = LoggerFactory.getLogger(InstagramPublisher.class);

    private final RestTemplate restTemplate;
    private final MetaOAuthProperties properties;

    public InstagramPublisher(RestTemplate restTemplate, MetaOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public boolean supports(Platform platform) {
        return platform != null && Platforms.INSTAGRAM_CODE.equalsIgnoreCase(platform.getCode());
    }

    @Override
    public PlatformPublishResult publish(PlatformPublishCommand command) {
        if (!supports(command.getPlatform())) {
            return PlatformPublishResult.failure("INSTAGRAM_UNSUPPORTED_PLATFORM", "Unsupported platform");
        }

        PlatformAuthContext auth = command.getAuthContext();
        PlatformConnection connection = command.getConnection();
        List<String> assets = command.getMediaAssetIds();
        String caption = command.getCaption();

        String igAccountId = resolveInstagramAccountId(connection);
        if (igAccountId == null || igAccountId.isBlank()) {
            return PlatformPublishResult.failure("INSTAGRAM_ACCOUNT_NOT_FOUND", "No Instagram account linked to this connection.");
        }
        if (assets == null || assets.isEmpty()) {
            return PlatformPublishResult.failure("INSTAGRAM_NO_MEDIA", "No media assets provided for publishing.");
        }

        MediaType mediaType = detectMediaType(command);

        try {
            MediaCreationResult creation = switch (mediaType) {
                case IMAGE -> createImageContainer(igAccountId, assets.get(0), caption, auth.getAccessToken());
                case VIDEO -> createVideoContainer(igAccountId, assets.get(0), caption, auth.getAccessToken());
                case REEL -> createReelContainer(igAccountId, assets.get(0), caption, auth.getAccessToken());
                case CAROUSEL -> createCarouselContainer(igAccountId, assets, caption, auth.getAccessToken());
            };
            if (creation.id == null) {
                return PlatformPublishResult.failure("INSTAGRAM_CREATE_FAILED",
                        creation.errorMessage != null ? creation.errorMessage : "Failed to create media container.");
            }
            ContainerStatus status = waitForContainerReady(creation.id, auth.getAccessToken());
            if (status == ContainerStatus.ERROR) {
                return PlatformPublishResult.failure("INSTAGRAM_CONTAINER_ERROR", "Media container failed to process. Check the media URL and permissions, then retry.");
            }
            if (status == ContainerStatus.PENDING) {
                return PlatformPublishResult.failure("INSTAGRAM_CONTAINER_NOT_READY", "Media is not ready yet. Please retry.");
            }

            PublishResult publish = publishMedia(igAccountId, creation.id, auth.getAccessToken());
            if (publish.id == null) {
                return PlatformPublishResult.failure("INSTAGRAM_PUBLISH_FAILED",
                        publish.errorMessage != null ? publish.errorMessage : "Failed to publish media.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("creationId", creation.id);
            response.put("publishId", publish.id);
            return PlatformPublishResult.success(publish.id, Instant.now(), response);
        } catch (RestClientException ex) {
            log.warn("Instagram publish failed userId={} connectionId={} error={}", command.getUserId(), connection.getId(), ex.getMessage());
            return PlatformPublishResult.failure("INSTAGRAM_API_ERROR", ex.getMessage());
        } catch (IllegalStateException ex) {
            log.warn("Instagram publish failed during carousel child creation userId={} connectionId={} error={}", command.getUserId(), connection.getId(), ex.getMessage());
            return PlatformPublishResult.failure("INSTAGRAM_CREATE_FAILED", ex.getMessage());
        }
    }

    private String resolveInstagramAccountId(PlatformConnection connection) {
        if (connection.getAccountMetadata() == null) {
            return null;
        }
        Object igId = connection.getAccountMetadata().get("instagramId");
        return igId != null ? igId.toString() : null;
    }

    private MediaCreationResult createImageContainer(String igAccountId, String mediaUrl, String caption, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), igAccountId, "media")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("image_url", mediaUrl);
        payload.put("media_type", "IMAGE");
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        payload.put("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        String error = extractError(response.getBody());
        return new MediaCreationResult(id != null ? id.toString() : null, error);
    }

    private MediaCreationResult createVideoContainer(String igAccountId, String mediaUrl, String caption, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), igAccountId, "media")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("media_type", "VIDEO");
        payload.put("video_url", mediaUrl);
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        payload.put("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        String error = extractError(response.getBody());
        return new MediaCreationResult(id != null ? id.toString() : null, error);
    }

    private MediaCreationResult createReelContainer(String igAccountId, String mediaUrl, String caption, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), igAccountId, "media")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("media_type", "REELS");
        payload.put("video_url", mediaUrl);
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        payload.put("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        String error = extractError(response.getBody());
        return new MediaCreationResult(id != null ? id.toString() : null, error);
    }

    private MediaCreationResult createCarouselContainer(String igAccountId, List<String> mediaUrls, String caption, String accessToken) {
        List<String> childIds = mediaUrls.stream()
                .map(url -> createImageContainer(igAccountId, url, null, accessToken))
                .map(result -> {
                    if (result.id == null) {
                        throw new IllegalStateException(result.errorMessage != null ? result.errorMessage : "Failed to create carousel child");
                    }
                    return result.id;
                })
                .toList();

        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), igAccountId, "media")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("media_type", "CAROUSEL");
        payload.put("children", String.join(",", childIds));
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        payload.put("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        String error = extractError(response.getBody());
        return new MediaCreationResult(id != null ? id.toString() : null, error);
    }

    private PublishResult publishMedia(String igAccountId, String creationId, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), igAccountId, "media_publish")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("creation_id", creationId);
        payload.put("access_token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        String error = extractError(response.getBody());
        return new PublishResult(id != null ? id.toString() : null, error);
    }

    /**
     * Optionally poll a container until Meta reports it is ready. This reduces "Media ID is not available" errors
     * when publishing immediately after creation.
     */
    private ContainerStatus waitForContainerReady(String creationId, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com")
                .pathSegment(properties.getApiVersion(), creationId)
                .queryParam("fields", "status_code,status")
                .queryParam("access_token", accessToken)
                .toUriString();
        int attempts = 0;
        while (attempts < 5) {
            attempts++;
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    break;
                }
                Object status = body.get("status_code");
                if (status != null) {
                    String s = status.toString();
                    if ("FINISHED".equalsIgnoreCase(s)) {
                        return ContainerStatus.READY;
                    }
                    if ("ERROR".equalsIgnoreCase(s)) {
                        return ContainerStatus.ERROR;
                    }
                }
                Thread.sleep(750);
            } catch (Exception ignored) {
                // continue retries
            }
        }
        return ContainerStatus.PENDING; // fall back to attempting publish
    }

    private String extractError(Map body) {
        if (body == null) {
            return null;
        }
        Object error = body.get("error");
        if (error instanceof Map<?, ?> errorMap) {
            Object msg = errorMap.get("message");
            return msg != null ? msg.toString() : errorMap.toString();
        }
        return null;
    }

    private MediaType detectMediaType(PlatformPublishCommand command) {
        List<String> assets = command.getMediaAssetIds();
        if (assets == null || assets.isEmpty()) {
            return MediaType.IMAGE;
        }

        Object hint = command.getMetadata() != null ? command.getMetadata().get("instagramMediaType") : null;
        if (hint instanceof String s) {
            try {
                return MediaType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // ignore and fall back
            }
        }

        if (assets.size() > 1) {
            return MediaType.CAROUSEL;
        }

        String url = assets.get(0).toLowerCase();
        if (url.endsWith(".mp4") || url.endsWith(".mov") || url.endsWith(".avi") || url.endsWith(".mkv")) {
            return MediaType.VIDEO;
        }

        return MediaType.IMAGE;
    }

    private record MediaCreationResult(String id, String errorMessage) {}
    private record PublishResult(String id, String errorMessage) {}
    private enum ContainerStatus { READY, ERROR, PENDING }
    private enum MediaType { IMAGE, VIDEO, REEL, CAROUSEL }
}
