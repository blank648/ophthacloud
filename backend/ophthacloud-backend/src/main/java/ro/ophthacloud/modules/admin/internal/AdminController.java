package ro.ophthacloud.modules.admin.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.admin.AdminFacade;
import ro.ophthacloud.modules.admin.dto.CreateStaffMemberRequest;
import ro.ophthacloud.modules.admin.dto.StaffMemberDto;
import ro.ophthacloud.modules.admin.dto.UpdateStaffMemberRequest;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PagedApiResponse;

import java.util.UUID;

/**
 * REST controller for staff management.
 * Adheres to GUIDE_04 §10.1.
 */
@RestController
@RequestMapping("/api/v1/admin/staff")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminFacade adminFacade;

    @GetMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'VIEW')")
    public PagedApiResponse<StaffMemberDto> listStaff(
            @RequestParam(required = false) StaffRole role,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("REST request to list staff [role={}, page={}]", role, pageable.getPageNumber());
        Page<StaffMemberDto> page = adminFacade.listStaffMembers(role, pageable);
        return PagedApiResponse.of(page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('admin', 'MODULE', 'CREATE')")
    public ApiResponse<StaffMemberDto> createStaff(@Valid @RequestBody CreateStaffMemberRequest request) {
        log.debug("REST request to create staff: {} {}", request.firstName(), request.lastName());
        StaffMemberDto staff = adminFacade.createStaffMember(request);
        return ApiResponse.of(staff);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('admin', 'MODULE', 'EDIT')")
    public ApiResponse<StaffMemberDto> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffMemberRequest request) {
        log.debug("REST request to update staff: {}", id);
        StaffMemberDto staff = adminFacade.updateStaffMember(id, request);
        return ApiResponse.of(staff);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('admin', 'MODULE', 'DELETE')")
    public void deleteStaff(@PathVariable UUID id) {
        log.debug("REST request to deactivate staff: {}", id);
        adminFacade.deactivateStaffMember(id);
    }
}
