package saas.personal_branding.api.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.ReferenceDataService;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.presentation.dto.response.CountryResponse;
import saas.personal_branding.api.presentation.dto.response.ReferenceDataResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reference-data")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping("/niches")
    public ResponseEntity<List<ReferenceDataResponse>> getNiches() {
        return ResponseEntity.ok(toReferenceResponse(referenceDataService.getNiches()));
    }

    @GetMapping("/audiences")
    public ResponseEntity<List<ReferenceDataResponse>> getAudiences() {
        return ResponseEntity.ok(toReferenceResponse(referenceDataService.getAudiences()));
    }

    @GetMapping("/tones")
    public ResponseEntity<List<ReferenceDataResponse>> getTones() {
        return ResponseEntity.ok(toReferenceResponse(referenceDataService.getTones()));
    }

    @GetMapping("/platforms")
    public ResponseEntity<List<ReferenceDataResponse>> getPlatforms() {
        return ResponseEntity.ok(toReferenceResponse(referenceDataService.getPlatforms()));
    }

    @GetMapping("/posting-frequencies")
    public ResponseEntity<List<ReferenceDataResponse>> getPostingFrequencies() {
        return ResponseEntity.ok(toReferenceResponse(referenceDataService.getPostingFrequencies()));
    }

    @GetMapping("/countries")
    public ResponseEntity<List<CountryResponse>> getCountries() {
        Set<Country> countries = referenceDataService.getCountries();
        List<CountryResponse> response = countries.stream()
                .sorted(Comparator.comparing(Country::getName, String.CASE_INSENSITIVE_ORDER))
                .map(country -> new CountryResponse(country.getId(), country.getName(), country.getIsoCode()))
                .toList();
        return ResponseEntity.ok(response);
    }

    private List<ReferenceDataResponse> toReferenceResponse(Set<? extends Object> items) {
        return items.stream()
                .map(item -> {
                    if (item instanceof Niche niche) {
                        return new ReferenceDataResponse(niche.getId(), niche.getName());
                    }
                    if (item instanceof Audience audience) {
                        return new ReferenceDataResponse(audience.getId(), audience.getName());
                    }
                    if (item instanceof Tone tone) {
                        return new ReferenceDataResponse(tone.getId(), tone.getName());
                    }
                    if (item instanceof Platform platform) {
                        return new ReferenceDataResponse(platform.getId(), platform.getName());
                    }
                    if (item instanceof PostingFrequency postingFrequency) {
                        return new ReferenceDataResponse(postingFrequency.getId(), postingFrequency.getName());
                    }
                    throw new IllegalArgumentException("Unsupported reference data type: " + item.getClass());
                })
                .sorted(Comparator.comparing(ReferenceDataResponse::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
