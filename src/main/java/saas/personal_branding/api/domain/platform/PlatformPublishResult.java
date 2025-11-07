package saas.personal_branding.api.domain.platform;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PlatformPublishResult {
    private final boolean success;
    private final String externalPostId;
    private final Instant publishedAt;
    private final String providerStatus;
    private final String errorCode;
    private final String errorMessage;
    @Singular("responseEntry")
    private final Map<String, Object> rawResponse;

    public static PlatformPublishResult success(String externalPostId, Instant publishedAt, Map<String, Object> rawResponse) {
        return PlatformPublishResult.builder()
                .success(true)
                .externalPostId(externalPostId)
                .publishedAt(publishedAt)
                .rawResponse(rawResponse)
                .build();
    }

    public static PlatformPublishResult failure(String errorCode, String errorMessage) {
        return PlatformPublishResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
