package saas.personal_branding.api.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import saas.personal_branding.api.application.service.AuthService;
import saas.personal_branding.api.application.service.LoginRateLimiter;
import saas.personal_branding.api.application.service.RefreshRateLimiter;
import saas.personal_branding.api.application.service.OnboardingService;
import saas.personal_branding.api.application.service.ReferenceDataService;
import saas.personal_branding.api.application.service.PasswordResetService;
import saas.personal_branding.api.application.service.PasswordResetNotifier;
import saas.personal_branding.api.application.service.EmailVerificationService;
import saas.personal_branding.api.application.service.EmailVerificationNotifier;
import saas.personal_branding.api.application.service.SecurityAuditLogger;
import saas.personal_branding.api.application.service.TokenHashService;
import saas.personal_branding.api.application.service.TokenService;
import saas.personal_branding.api.domain.repository.PersonRepository;
import saas.personal_branding.api.domain.repository.ReferenceDataRepository;
import saas.personal_branding.api.domain.repository.RefreshTokenRepository;
import saas.personal_branding.api.domain.repository.PasswordResetTokenRepository;
import saas.personal_branding.api.domain.repository.EmailVerificationTokenRepository;
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
                                  TokenHashService tokenHashService,
                                  Clock clock,
                                   JwtProperties jwtProperties,
                                   LoginRateLimiter loginRateLimiter,
                                   RefreshRateLimiter refreshRateLimiter,
                                   EmailVerificationService emailVerificationService,
                                   MeterRegistry meterRegistry,
                                   SecurityAuditLogger securityAuditLogger) {
        java.time.Duration refreshTtl = jwtProperties.refreshTokenTtl() != null
                ? jwtProperties.refreshTokenTtl()
                : java.time.Duration.ofDays(7);
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, tokenService, tokenHashService, clock, refreshTtl, loginRateLimiter, refreshRateLimiter, emailVerificationService, meterRegistry, securityAuditLogger);
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
    public PasswordResetService passwordResetService(UserRepository userRepository,
                                                     PasswordResetTokenRepository passwordResetTokenRepository,
                                                     PasswordEncoder passwordEncoder,
                                                     TokenHashService tokenHashService,
                                                     Clock clock,
                                                     @Value("${security.password-reset.token-ttl:PT15M}") java.time.Duration tokenTtl,
                                                     PasswordResetNotifier notifier,
                                                     @Value("${app.mail.reset-base-url}") String resetBaseUrl) {
        return new PasswordResetService(userRepository, passwordResetTokenRepository, passwordEncoder, tokenHashService, clock, tokenTtl, notifier, resetBaseUrl);
    }

    @Bean
    public EmailVerificationService emailVerificationService(UserRepository userRepository,
                                                             EmailVerificationTokenRepository emailVerificationTokenRepository,
                                                             TokenHashService tokenHashService,
                                                             EmailVerificationNotifier notifier,
                                                             Clock clock,
                                                             @Value("${app.mail.verify-base-url}") String verifyBaseUrl,
                                                             @Value("${security.email-verification.token-ttl:PT24H}") java.time.Duration tokenTtl,
                                                             SecurityAuditLogger securityAuditLogger) {
        return new EmailVerificationService(userRepository, emailVerificationTokenRepository, tokenHashService, notifier, clock, verifyBaseUrl, tokenTtl, securityAuditLogger);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
