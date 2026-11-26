package ro.unibuc.fmi.ophthacloud.patientbooking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PreferencesRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("2.0") @Digits(integer = 1, fraction = 1) BigDecimal uiFontScale,
        @NotNull Boolean highContrast) {}
