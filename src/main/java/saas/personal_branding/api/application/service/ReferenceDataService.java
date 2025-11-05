package saas.personal_branding.api.application.service;

import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;

import java.util.Set;

@Transactional(readOnly = true)
public class ReferenceDataService {

    private final ReferenceDataRepository referenceDataRepository;

    public ReferenceDataService(ReferenceDataRepository referenceDataRepository) {
        this.referenceDataRepository = referenceDataRepository;
    }

    public Set<Niche> getNiches() {
        return referenceDataRepository.findAllNiches();
    }

    public Set<Audience> getAudiences() {
        return referenceDataRepository.findAllAudiences();
    }

    public Set<Tone> getTones() {
        return referenceDataRepository.findAllTones();
    }

    public Set<Platform> getPlatforms() {
        return referenceDataRepository.findAllPlatforms();
    }

    public Set<Country> getCountries() {
        return referenceDataRepository.findAllCountries();
    }

    public Set<PostingFrequency> getPostingFrequencies() {
        return referenceDataRepository.findAllPostingFrequencies();
    }
}
