package ro.ophthacloud.modules.admin.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.admin.AdminFacade;
import ro.ophthacloud.modules.admin.dto.ClinicSettingsDto;
import ro.ophthacloud.modules.admin.dto.UpdateClinicSettingsRequest;
import ro.ophthacloud.shared.api.ApiResponse;

/**
 * REST controller for clinic settings.
 * Adheres to GUIDE_04 §10.3.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class ClinicSettingsController {

    private final AdminFacade adminFacade;

    @GetMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'VIEW')")
    public ApiResponse<ClinicSettingsDto> getSettings() {
        log.debug("REST request to get clinic settings");
        ClinicSettingsDto settings = adminFacade.getClinicSettings();
        return ApiResponse.of(settings);
    }

    @PutMapping
    @PreAuthorize("hasPermission('admin', 'MODULE', 'EDIT')")
    public ApiResponse<ClinicSettingsDto> updateSettings(
            @Valid @RequestBody UpdateClinicSettingsRequest request) {
        log.debug("REST request to update clinic settings");
        ClinicSettingsDto updated = adminFacade.updateClinicSettings(request);
        return ApiResponse.of(updated);
    }
}
