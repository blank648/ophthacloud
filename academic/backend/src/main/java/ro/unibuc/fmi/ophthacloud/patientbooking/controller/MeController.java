package ro.unibuc.fmi.ophthacloud.patientbooking.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.fmi.ophthacloud.patientbooking.dto.*;
import ro.unibuc.fmi.ophthacloud.patientbooking.service.MePreferencesService;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final MePreferencesService preferences;

    public MeController(MePreferencesService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public CurrentUserResponse me() {
        return preferences.me();
    }

    @PatchMapping("/preferences")
    public CurrentUserResponse update(@Valid @RequestBody PreferencesRequest request) {
        return preferences.update(request);
    }
}
