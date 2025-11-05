package saas.personal_branding.api.presentation.controller;

import jakarta.validation.Valid;
import saas.personal_branding.api.application.service.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.personal_branding.api.application.service.OnboardingService;
import saas.personal_branding.api.domain.model.Person;
import saas.personal_branding.api.presentation.dto.request.OnboardingRequest;
import saas.personal_branding.api.presentation.dto.response.PersonResponse;
import saas.personal_branding.api.presentation.mapper.UserDtoMapper;

@RestController
@RequestMapping("/api/onboarding")
@Validated
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public OnboardingController(OnboardingService onboardingService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.onboardingService = onboardingService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping
    public ResponseEntity<PersonResponse> completeOnboarding(@Valid @RequestBody OnboardingRequest request) {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        Person person = onboardingService.completeOnboarding(new OnboardingService.CompleteOnboardingCommand(
                userId,
                request.getFullName(),
                request.getPhoneNumber(),
                request.getCompanyName(),
                request.getPosition(),
                request.getBrandColor(),
                request.getFontStyle(),
                request.getNicheIds(),
                request.getAudienceIds(),
                request.getToneIds(),
                request.getPlatformIds(),
                request.getCountryIds(),
                request.getPostingFrequencyIds()
        ));

        return ResponseEntity.ok(UserDtoMapper.toPersonResponse(person));
    }

    @GetMapping
    public ResponseEntity<PersonResponse> getOnboarding() {
        Long userId = authenticatedUserProvider.getCurrentUserId();
        PersonResponse response = onboardingService.findPersonByUserId(userId)
                .map(UserDtoMapper::toPersonResponse)
                .orElse(UserDtoMapper.emptyPersonResponse(userId));

        return ResponseEntity.ok(response);
    }

}
