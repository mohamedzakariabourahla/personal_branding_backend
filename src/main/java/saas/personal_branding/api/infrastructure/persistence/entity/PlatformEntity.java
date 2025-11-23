package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platforms")
@Getter
@Setter
public class PlatformEntity extends ReferenceDataEntity {

    @Column(nullable = false, unique = true)
    private String code;
}
