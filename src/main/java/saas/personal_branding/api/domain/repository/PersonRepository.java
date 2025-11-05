package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.Person;

import java.util.Optional;

public interface PersonRepository {
    Person save(Person person);
    Optional<Person> findByUserId(Long userId);
}
