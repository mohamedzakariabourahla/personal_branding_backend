package saas.personal_branding.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.OnboardingService;
import saas.personal_branding.api.application.service.ReferenceDataService;
import saas.personal_branding.api.application.service.TokenService;
import saas.personal_branding.api.domain.repository.PersonRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.domain.repository.RefreshTokenRepository;
import saas.personal_branding.api.domain.repository.UserRepository;
import saas.personal_branding.api.infrastructure.security.JwtProperties;

import java.time.Clock;

@Configuration
public class BeanConfig {

    @Bean
    public AuthService authService(UserRepository userRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  TokenService tokenService,
                                  Clock clock,
                                  JwtProperties jwtProperties) {
        java.time.Duration refreshTtl = jwtProperties.refreshTokenTtl() != null
                ? jwtProperties.refreshTokenTtl()
                : java.time.Duration.ofDays(7);
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, tokenService, clock, refreshTtl);
    }

    @Bean
    public OnboardingService onboardingService(UserRepository userRepository,
                                               PersonRepository personRepository,
                                               ReferenceDataRepository referenceDataRepository) {
        return new OnboardingService(userRepository, personRepository, referenceDataRepository);
    }

    @Bean
    public ReferenceDataService referenceDataService(ReferenceDataRepository referenceDataRepository) {
        return new ReferenceDataService(referenceDataRepository);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
