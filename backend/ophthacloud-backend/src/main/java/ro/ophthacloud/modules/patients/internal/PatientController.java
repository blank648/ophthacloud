package ro.ophthacloud.modules.patients.internal;

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
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.patients.dto.PatientSummaryDto;
import ro.ophthacloud.modules.patients.dto.UpdatePatientRequest;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PagedApiResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST Endpoint for Patients module.
 * Adheres to GUIDE_04 §2.
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientManagementFacade patientManagementFacade;

    @GetMapping
    @PreAuthorize("hasPermission('patients', 'MODULE', 'VIEW')")
    public PagedApiResponse<PatientSummaryDto> listPatients(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("REST request to list patients [q='{}', page={}]", q, pageable.getPageNumber());
        Page<PatientSummaryDto> page = patientManagementFacade.listPatients(q, pageable);
        return PagedApiResponse.of(page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('patients', 'MODULE', 'CREATE')")
    public ApiResponse<PatientDto> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        log.debug("REST request to create patient: {} {}", request.getFirstName(), request.getLastName());
        PatientDto patient = patientManagementFacade.createPatient(request);
        return ApiResponse.of(patient);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('patients', 'MODULE', 'VIEW')")
    public ApiResponse<PatientDto> getPatient(@PathVariable UUID id) {
        log.debug("REST request to get patient: {}", id);
        PatientDto patient = patientManagementFacade.getPatient(id);
        return ApiResponse.of(patient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('patients', 'MODULE', 'EDIT')")
    public ApiResponse<PatientDto> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientRequest request) {
        log.debug("REST request to update patient: {}", id);
        PatientDto patient = patientManagementFacade.updatePatient(id, request);
        return ApiResponse.of(patient);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('patients', 'MODULE', 'DELETE')")
    public void deletePatient(@PathVariable UUID id) {
        log.debug("REST request to delete patient: {}", id);
        patientManagementFacade.deletePatient(id);
    }

    @PostMapping("/{id}/portal-invite")
    @PreAuthorize("hasPermission('patients', 'MODULE', 'EDIT')")
    public ApiResponse<Map<String, Instant>> inviteToPortal(@PathVariable UUID id) {
        log.debug("REST request to invite patient to portal: {}", id);
        Instant invitedAt = patientManagementFacade.inviteToPortal(id);
        return ApiResponse.of(Map.of("invitedAt", invitedAt));
    }
}
