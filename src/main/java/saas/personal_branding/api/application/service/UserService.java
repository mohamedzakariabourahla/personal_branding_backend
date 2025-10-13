package saas.personal_branding.api.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import saas.personal_branding.api.application.exception.UserException;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.UserRepository;
import saas.personal_branding.api.infrastructure.persistence.repositoryAdapter.UserRepositoryAdapter;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User creatUser(String fullName, String email, String plainPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserException.EmailAlreadyExistsException(email);
        }
        String hashedPassword = passwordEncoder.encode(plainPassword);
        User user = new User(null, fullName, email, hashedPassword);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User authenticate(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserException.InvalidCredentialsException::new);

        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            throw new UserException.InvalidCredentialsException();
        }

        return user;
    }


}
