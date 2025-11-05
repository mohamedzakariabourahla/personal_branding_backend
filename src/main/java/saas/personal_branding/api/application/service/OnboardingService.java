package saas.personal_branding.api.application.service;

import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.exception.ReferenceDataException;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.*;
import saas.personal_branding.api.domain.repository.PersonRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.domain.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class OnboardingService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final ReferenceDataRepository referenceDataRepository;

    public OnboardingService(UserRepository userRepository,
                             PersonRepository personRepository,
                             ReferenceDataRepository referenceDataRepository) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    public Person completeOnboarding(CompleteOnboardingCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException.UserNotFoundException(command.userId()));

        if (!user.isActive()) {
            throw new UserException.InactiveAccountException(user.getId());
        }

        if (user.isOnboardingCompleted()) {
            throw new UserException.OnboardingAlreadyCompletedException(user.getId());
        }

        Set<Long> nicheIds = defaultIds(command.nicheIds(), "nicheIds");
        Set<Niche> niches = referenceDataRepository.findNichesByIds(nicheIds);
        ensureAllRequestedPresent("Niche", nicheIds, niches, Niche::getId);

        Set<Long> audienceIds = defaultIds(command.audienceIds(), "audienceIds");
        Set<Audience> audiences = referenceDataRepository.findAudiencesByIds(audienceIds);
        ensureAllRequestedPresent("Audience", audienceIds, audiences, Audience::getId);

        Set<Long> toneIds = defaultIds(command.toneIds(), "toneIds");
        Set<Tone> tones = referenceDataRepository.findTonesByIds(toneIds);
        ensureAllRequestedPresent("Tone", toneIds, tones, Tone::getId);

        Set<Long> platformIds = defaultIds(command.platformIds(), "platformIds");
        Set<Platform> platforms = referenceDataRepository.findPlatformsByIds(platformIds);
        ensureAllRequestedPresent("Platform", platformIds, platforms, Platform::getId);

        Set<Long> countryIds = defaultIds(command.countryIds(), "countryIds");
        Set<Country> countries = referenceDataRepository.findCountriesByIds(countryIds);
        ensureAllRequestedPresent("Country", countryIds, countries, Country::getId);

        Set<Long> postingFrequencyIds = defaultIds(command.postingFrequencyIds(), "postingFrequencyIds");
        Set<PostingFrequency> postingFrequencies = referenceDataRepository.findPostingFrequenciesByIds(postingFrequencyIds);
        ensureAllRequestedPresent("PostingFrequency", postingFrequencyIds, postingFrequencies, PostingFrequency::getId);

        Person person = Person.builder()
                .userId(user.getId())
                .fullName(command.fullName())
                .phoneNumber(command.phoneNumber())
                .companyName(command.companyName())
                .position(command.position())
                .brandColor(command.brandColor())
                .fontStyle(command.fontStyle())
                .niches(niches)
                .audiences(audiences)
                .tones(tones)
                .platforms(platforms)
                .countries(countries)
                .postingFrequencies(postingFrequencies)
                .build();

        Person savedPerson = personRepository.save(person);
        userRepository.updateOnboardingStatus(user.getId(), OnboardingStatus.COMPLETED);

        return savedPerson;
    }

    private <T, ID> void ensureAllRequestedPresent(String type,
                                                   Set<ID> requestedIds,
                                                   Set<T> foundEntities,
                                                   Function<T, ID> idExtractor) {
        Set<ID> normalizedRequested = requestedIds == null ? Set.of() : Set.copyOf(requestedIds);
        if (normalizedRequested.isEmpty()) {
            return;
        }

        Set<ID> foundIds = foundEntities.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!foundIds.containsAll(normalizedRequested)) {
            Set<ID> missing = normalizedRequested.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ReferenceDataException.ReferenceDataNotFoundException(type, missing);
        }
    }

    private Set<Long> defaultIds(Set<Long> ids, String fieldName) {
        if (ids == null) {
            return Set.of();
        }
        Set<Long> sanitized = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sanitized.size() != ids.size()) {
            throw new IllegalArgumentException("Property '%s' contains null elements".formatted(fieldName));
        }
        return Set.copyOf(sanitized);
    }

    @Transactional(readOnly = true)
    public Optional<Person> findPersonByUserId(Long userId) {
        return personRepository.findByUserId(userId);
    }

    public record CompleteOnboardingCommand(
            Long userId,
            String fullName,
            String phoneNumber,
            String companyName,
            String position,
            String brandColor,
            String fontStyle,
            Set<Long> nicheIds,
            Set<Long> audienceIds,
            Set<Long> toneIds,
            Set<Long> platformIds,
            Set<Long> countryIds,
            Set<Long> postingFrequencyIds) {
    }
}
