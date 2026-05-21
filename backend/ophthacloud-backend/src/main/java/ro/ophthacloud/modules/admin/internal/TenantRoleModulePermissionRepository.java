package ro.ophthacloud.modules.admin.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRoleModulePermissionRepository extends JpaRepository<TenantRoleModulePermissionEntity, UUID> {

    List<TenantRoleModulePermissionEntity> findByTenantIdAndRole(UUID tenantId, StaffRole role);

    void deleteByTenantIdAndRole(UUID tenantId, StaffRole role);
}
