package ro.ophthacloud.modules.admin.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.admin.AdminFacade;
import ro.ophthacloud.modules.admin.dto.PermissionMatrixDto;
import ro.ophthacloud.modules.admin.dto.UpdatePermissionsRequest;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.List;

/**
 * REST controller for RBAC permissions matrix.
 * Adheres to GUIDE_04 §10.2.
 */
@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionsController {

    private final AdminFacade adminFacade;

    @GetMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'VIEW')")
    public ApiResponse<List<PermissionMatrixDto>> getPermissions(@RequestParam StaffRole role) {
        log.debug("REST request to get permissions for role: {}", role);
        List<PermissionMatrixDto> permissions = adminFacade.getPermissionsByRole(role);
        return ApiResponse.of(permissions);
    }

    @PutMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'EDIT')")
    public ApiResponse<List<PermissionMatrixDto>> updatePermissions(
            @Valid @RequestBody UpdatePermissionsRequest request) {
        log.debug("REST request to update permissions for role: {}", request.role());
        List<PermissionMatrixDto> updated = adminFacade.updatePermissions(request);
        return ApiResponse.of(updated);
    }
}
