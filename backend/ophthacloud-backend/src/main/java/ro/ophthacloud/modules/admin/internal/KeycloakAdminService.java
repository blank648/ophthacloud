package ro.ophthacloud.modules.admin.internal;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloak;

    @Value("${ophthacloud.keycloak.realm}")
    private String realm;

    public String createUser(String email, String firstName, String lastName) {
        UsersResource usersResource = keycloak.realm(realm).users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == 201) {
                String path = response.getLocation().getPath();
                String userId = path.substring(path.lastIndexOf('/') + 1);
                log.info("Created Keycloak user with ID: {}", userId);
                return userId;
            } else {
                log.error("Failed to create Keycloak user. Status: {}, Info: {}", response.getStatus(), response.getStatusInfo());
                throw new RuntimeException("Failed to create Keycloak user. Status: " + response.getStatus());
            }
        }
    }

    public void updateUser(String userId, String firstName, String lastName) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userResource.update(user);
        log.info("Updated Keycloak user {}", userId);
    }

    public void assignTenantMemberRole(String userId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);
        
        RoleRepresentation tenantMemberRole = realmResource.roles().get("TENANT_MEMBER").toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(tenantMemberRole));
        log.info("Assigned TENANT_MEMBER role to user {}", userId);
    }

    public void sendVerifyEmail(String userId) {
        try {
            keycloak.realm(realm).users().get(userId).sendVerifyEmail();
            log.info("Sent verification email to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send verification email to user {}: {}", userId, e.getMessage());
            // Requirement: "failure of email send does NOT rollback the user creation (email is best-effort)"
        }
    }

    public void setAttributes(String userId, UUID tenantId, UUID staffId, StaffRole role, String permissionsJson) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        attributes.put("tenant_id", Collections.singletonList(tenantId.toString()));
        attributes.put("staff_id", Collections.singletonList(staffId.toString()));
        attributes.put("staff_role", Collections.singletonList(role.name()));
        if (permissionsJson != null) {
            attributes.put("permissions_json", Collections.singletonList(permissionsJson));
        }

        user.setAttributes(attributes);
        userResource.update(user);
        log.info("Updated attributes for user {}", userId);
    }

    public void deleteUser(String userId) {
        try {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("Deleted Keycloak user {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user {}", userId, e);
        }
    }

    public void disableUser(String userId) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);
        log.info("Disabled Keycloak user {}", userId);
    }
    
    public void invalidateSessions(String userId) {
        keycloak.realm(realm).users().get(userId).logout();
        log.info("Invalidated sessions for Keycloak user {}", userId);
    }
}
