package liveproject.m2k8s.repository;

import liveproject.m2k8s.model.Profile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends CrudRepository<Profile, Long> {

  Optional<Profile> findByUsername(String username);

}
