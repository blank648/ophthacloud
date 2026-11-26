package ro.unibuc.fmi.ophthacloud.securityaudit.dto;

import ro.unibuc.fmi.ophthacloud.securityaudit.api.UserRole;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserRole role) {}
