package ro.unibuc.fmi.ophthacloud.securityaudit.api;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, UserRole role) {}
