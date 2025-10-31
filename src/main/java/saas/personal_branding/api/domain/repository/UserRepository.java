package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.OnboardingStatus;
import saas.personal_branding.api.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    List<User> findAll();
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User updateOnboardingStatus(Long userId, OnboardingStatus status);
}
