package ro.unibuc.fmi.ophthacloud.patientbooking.service;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import ro.unibuc.fmi.ophthacloud.common.ResourceNotFoundException;
import ro.unibuc.fmi.ophthacloud.patientbooking.dto.CurrentUserResponse;
import ro.unibuc.fmi.ophthacloud.patientbooking.dto.PreferencesRequest;
import ro.unibuc.fmi.ophthacloud.patientbooking.entity.PatientEntity;
import ro.unibuc.fmi.ophthacloud.patientbooking.repository.PatientRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MePreferencesService {
    private final CurrentUserProvider currentUser;
    private final PatientRepository patients;

    public MePreferencesService(CurrentUserProvider currentUser, PatientRepository patients) {
        this.currentUser = currentUser;
        this.patients = patients;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        var user = currentUser.currentUser();
        if (user.role() != UserRole.PATIENT) {
            return new CurrentUserResponse(user.userId(), user.email(), user.role(), null, new BigDecimal("1.0"), false);
        }
        return response(user, patient(user));
    }

    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    public CurrentUserResponse update(@Valid PreferencesRequest request) {
        var user = currentUser.currentUser();
        var patient = patients.findLockedByUserId(user.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        patient.updatePreferences(request.uiFontScale(), request.highContrast());
        return response(user, patient);
    }

    private PatientEntity patient(AuthenticatedUser user) {
        return patients.findByUserId(user.userId()).orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    private CurrentUserResponse response(AuthenticatedUser user, PatientEntity patient) {
        return new CurrentUserResponse(user.userId(), user.email(), user.role(), patient.getId(),
                patient.getUiFontScale(), patient.isHighContrast());
    }
}
