package ro.unibuc.fmi.ophthacloud.securityaudit.entity;

import jakarta.persistence.*;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.AuthenticatedUser;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.UserRole;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    @Column(name = "is_active", nullable = false)
    private boolean active;

    public AuthenticatedUser identity() {
        return new AuthenticatedUser(id, email, role);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }
}
