package ro.ophthacloud.modules.appointments.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

/**
 * JPA entity for {@code appointment_types}.
 * Each tenant manages their own set of appointment type templates (e.g. "Control Glaucom").
 */
@Entity
@Table(name = "appointment_types")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentTypeEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex = "#13759C";

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
