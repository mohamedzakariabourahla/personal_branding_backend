package saas.personal_branding.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "position")
    private String position;

    @Column(name = "brand_color")
    private String brandColor;

    @Column(name = "font_style")
    private String fontStyle;

    @ManyToMany
    @JoinTable(
            name = "person_niches",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "niche_id")
    )
    @Builder.Default
    private Set<NicheEntity> niches = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "person_audiences",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "audience_id")
    )
    @Builder.Default
    private Set<AudienceEntity> audiences = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "person_tones",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "tone_id")
    )
    @Builder.Default
    private Set<ToneEntity> tones = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "person_platforms",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @Builder.Default
    private Set<PlatformEntity> platforms = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "person_countries",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    @Builder.Default
    private Set<CountryEntity> countries = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "person_posting_frequencies",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "posting_frequency_id")
    )
    @Builder.Default
    private Set<PostingFrequencyEntity> postingFrequencies = new HashSet<>();
}
