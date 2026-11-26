package ro.unibuc.fmi.ophthacloud.securityaudit.dto;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(@NotNull String email, @NotNull String password) {}
