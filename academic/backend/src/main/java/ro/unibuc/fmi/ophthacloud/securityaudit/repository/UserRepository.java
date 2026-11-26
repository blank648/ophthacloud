package ro.unibuc.fmi.ophthacloud.securityaudit.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByIdAndActiveTrue(UUID id);
}
