package saas.personal_branding.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.OnboardingService;
import saas.personal_branding.api.domain.repository.PersonRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.domain.repository.UserRepository;

@Configuration
public class BeanConfig {

    @Bean
    public AuthService authService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new AuthService(userRepository, passwordEncoder);
    }

    @Bean
    public OnboardingService onboardingService(UserRepository userRepository,
                                               PersonRepository personRepository,
                                               ReferenceDataRepository referenceDataRepository) {
        return new OnboardingService(userRepository, personRepository, referenceDataRepository);
    }
}
