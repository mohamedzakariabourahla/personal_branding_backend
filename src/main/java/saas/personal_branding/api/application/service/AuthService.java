package saas.personal_branding.api.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.UserRepository;

@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserException.EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.builder()
                .email(command.email())
                .passwordHash(passwordHash)
                .active(true)
                .onboardingStatus(OnboardingStatus.NOT_STARTED)
                .role(Role.CLIENT)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(UserException.InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new UserException.InvalidCredentialsException();
        }

        return user;
    }

    public record RegisterUserCommand(String email, String password) {
    }

    public record LoginCommand(String email, String password) {
    }
}
