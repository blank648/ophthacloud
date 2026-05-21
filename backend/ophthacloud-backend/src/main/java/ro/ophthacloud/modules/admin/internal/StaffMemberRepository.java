package ro.ophthacloud.modules.admin.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMemberEntity, UUID> {

    Page<StaffMemberEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<StaffMemberEntity> findByTenantIdAndRole(UUID tenantId, StaffRole role, Pageable pageable);

    Optional<StaffMemberEntity> findByTenantIdAndKeycloakUserId(UUID tenantId, String keycloakUserId);

    Optional<StaffMemberEntity> findByTenantIdAndEmail(UUID tenantId, String email);
}
