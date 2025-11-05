package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class Country {
    private final Long id;
    private final String name;
    private final String isoCode;
}
