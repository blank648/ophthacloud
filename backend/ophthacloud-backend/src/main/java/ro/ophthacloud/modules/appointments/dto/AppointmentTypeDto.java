package ro.ophthacloud.modules.appointments.dto;

import ro.ophthacloud.modules.appointments.internal.AppointmentTypeEntity;

import java.util.UUID;

/** Response DTO for appointment types. */
public record AppointmentTypeDto(
        UUID id,
        String name,
        String colorHex,
        int durationMinutes,
        String description,
        boolean isActive
) {
    public static AppointmentTypeDto from(AppointmentTypeEntity e) {
        return new AppointmentTypeDto(
                e.getId(),
                e.getName(),
                e.getColorHex(),
                e.getDurationMinutes(),
                e.getDescription(),
                e.isActive()
        );
    }
}
