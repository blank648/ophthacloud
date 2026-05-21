package ro.ophthacloud.modules.admin.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicSettingsRepository extends JpaRepository<ClinicSettingsEntity, UUID> {

    Optional<ClinicSettingsEntity> findByTenantId(UUID tenantId);
}
