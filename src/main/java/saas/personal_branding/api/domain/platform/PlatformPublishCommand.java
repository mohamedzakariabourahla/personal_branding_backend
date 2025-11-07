package saas.personal_branding.api.domain.platform;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PlatformPublishCommand {
    private final Long userId;
    private final Platform platform;
    private final PlatformConnection connection;
    private final PlatformAuthContext authContext;
    @Singular
    private final List<String> mediaAssetIds;
    private final String caption;
    private final Instant scheduledAt;
    @Singular("metadataEntry")
    private final Map<String, Object> metadata;
}
