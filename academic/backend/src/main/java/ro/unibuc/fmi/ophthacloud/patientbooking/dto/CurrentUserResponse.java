package ro.unibuc.fmi.ophthacloud.patientbooking.dto;

import java.math.BigDecimal;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.UserRole;

public record CurrentUserResponse(UUID userId, String email, UserRole role, UUID patientId,
                                  BigDecimal uiFontScale, boolean highContrast) {}
