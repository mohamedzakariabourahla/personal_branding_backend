package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "countries")
@Getter
@Setter
public class CountryEntity extends ReferenceDataEntity {

    @Column(name = "iso_code", nullable = false, length = 3)
    private String isoCode;
}
