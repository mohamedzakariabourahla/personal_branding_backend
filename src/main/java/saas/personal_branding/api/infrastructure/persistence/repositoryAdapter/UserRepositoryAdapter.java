package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.repository.UserRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity(user.getFullName(), user.getEmail(), user.getPassword());
        UserEntity saved = jpaUserRepository.save(entity);
        return new User(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getPassword());
    }

    @Override
    public List<User> findAll() {
        return jpaUserRepository.findAll()
                .stream()
                .map(e -> new User(e.getId(), e.getFullName(), e.getEmail(), e.getPassword()))
                .toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(e -> new User(e.getId(), e.getFullName(), e.getEmail(), e.getPassword()));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(e -> new User(e.getId(), e.getFullName(), e.getEmail(), e.getPassword()));
    }

}
