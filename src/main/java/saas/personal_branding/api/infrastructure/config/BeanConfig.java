package saas.personal_branding.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import saas.personal_branding.api.application.service.UserService;
import saas.personal_branding.api.domain.repository.UserRepository;

@Configuration
public class BeanConfig {

    @Bean
    public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new UserService(userRepository, passwordEncoder);
    }
}
