package ro.ophthacloud.modules.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.ClinicSettingsDto;
import ro.ophthacloud.modules.admin.dto.CreateStaffMemberRequest;
import ro.ophthacloud.modules.admin.dto.PermissionMatrixDto;
import ro.ophthacloud.modules.admin.dto.StaffMemberDto;
import ro.ophthacloud.modules.admin.dto.UpdateClinicSettingsRequest;
import ro.ophthacloud.modules.admin.dto.UpdatePermissionsRequest;
import ro.ophthacloud.modules.admin.dto.UpdateStaffMemberRequest;
import ro.ophthacloud.modules.admin.internal.AdminService;
import ro.ophthacloud.modules.admin.internal.ClinicSettingsService;
import ro.ophthacloud.modules.admin.internal.PermissionsService;
import ro.ophthacloud.modules.admin.internal.StaffMemberRepository;
import ro.ophthacloud.modules.admin.internal.StaffRole;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.List;
import java.util.UUID;

/**
 * The ONLY public API of the admin module.
 * <p>
 * All other classes within this module are package-private or in {@code internal/} and must
 * never be injected directly from other modules — they communicate exclusively through this facade.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {

    private final AdminService adminService;
    private final PermissionsService permissionsService;
    private final ClinicSettingsService clinicSettingsService;
    private final StaffMemberRepository staffMemberRepository;

    // ── Staff Management ────────────────────────────────────────────────────

    @Transactional
    public StaffMemberDto createStaffMember(CreateStaffMemberRequest request) {
        return adminService.createStaffMember(request);
    }

    @Transactional
    public StaffMemberDto updateStaffMember(UUID staffId, UpdateStaffMemberRequest request) {
        return adminService.updateStaffMember(staffId, request);
    }

    @Transactional
    public void deactivateStaffMember(UUID staffId) {
        adminService.deactivateStaffMember(staffId);
    }

    public Page<StaffMemberDto> listStaffMembers(StaffRole role, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        if (role != null) {
            return staffMemberRepository.findByTenantIdAndRole(tenantId, role, pageable)
                    .map(StaffMemberDto::from);
        } else {
            return staffMemberRepository.findByTenantId(tenantId, pageable)
                    .map(StaffMemberDto::from);
        }
    }

    // ── Permissions Matrix ──────────────────────────────────────────────────

    public List<PermissionMatrixDto> getPermissionsByRole(StaffRole role) {
        return permissionsService.getPermissionsByRole(role);
    }

    @Transactional
    public List<PermissionMatrixDto> updatePermissions(UpdatePermissionsRequest request) {
        return permissionsService.updatePermissions(request);
    }

    // ── Clinic Settings ─────────────────────────────────────────────────────

    public ClinicSettingsDto getClinicSettings() {
        return clinicSettingsService.getClinicSettings();
    }

    @Transactional
    public ClinicSettingsDto updateClinicSettings(UpdateClinicSettingsRequest request) {
        return clinicSettingsService.updateClinicSettings(request);
    }
}
