package ro.unibuc.fmi.ophthacloud.clinicalamsler.api;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TreatmentPlanSnapshotDto(
        UUID planId,
        UUID patientId,
        String medicationName,
        TargetEye targetEye,
        int dropsPerDose,
        List<String> scheduleHours,
        LocalDate startDate,
        LocalDate endDate,
        Instant activatedAt,
        @Nullable Instant stoppedAt,
        boolean active
) {
    public TreatmentPlanSnapshotDto {
        scheduleHours = scheduleHours == null ? List.of() : List.copyOf(scheduleHours);
    }
}
