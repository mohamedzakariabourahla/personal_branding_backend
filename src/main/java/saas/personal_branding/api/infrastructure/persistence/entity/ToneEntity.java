package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tones")
@Getter
@Setter
public class ToneEntity extends ReferenceDataEntity {
}
