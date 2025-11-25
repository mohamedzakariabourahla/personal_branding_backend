package saas.personal_branding.api.publishing.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishingJobRequest {

    @NotNull
    private Long platformId;

    @NotNull
    private Long connectionId;

    @NotEmpty
    private List<String> mediaAssetIds;

    private String caption;

    /**
     * Optional scheduled time; if null the job is scheduled immediately.
     */
    private Instant scheduledAt;
}
