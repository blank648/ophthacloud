package ro.ophthacloud.modules.admin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.time.Instant;

@Entity
@Table(name = "staff_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberEntity extends TenantAwareEntity {

    @Column(name = "keycloak_user_id", nullable = false, length = 256)
    private String keycloakUserId;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "email", nullable = false, length = 256)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private StaffRole role;

    @Column(name = "specialization", length = 256)
    private String specialization;

    @Column(name = "license_number", length = 128)
    private String licenseNumber;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
