package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import ro.ophthacloud.shared.audit.AuditEntry;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Listens for successful Spring Security authentication events.
 * Since OphthaCloud is a stateless REST API, every API call validates the Bearer token and fires
 * an {@link AuthenticationSuccessEvent}.
 * To avoid database write and audit logging floods, we check if the staff member's lastLoginAt
 * has been updated in the last 5 minutes. If not, we update it and log the `LOGIN` action.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginEventListener {

    private final StaffMemberRepository staffMemberRepository;
    private final AuditLogService auditLogService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof OphthaClinicalAuthenticationToken token) {
            OphthaPrincipal principal = (OphthaPrincipal) token.getPrincipal();
            if (principal.staffId() != null && !principal.staffId().isBlank()) {
                try {
                    UUID staffId = UUID.fromString(principal.staffId());
                    staffMemberRepository.findById(staffId).ifPresent(staff -> {
                        Instant now = Instant.now();
                        if (staff.getLastLoginAt() == null || staff.getLastLoginAt().isBefore(now.minus(5, ChronoUnit.MINUTES))) {
                            staff.setLastLoginAt(now);
                            staffMemberRepository.save(staff);

                            auditLogService.log(AuditEntry.builder()
                                    .action("LOGIN")
                                    .entityType("StaffMember")
                                    .entityId(staffId)
                                    .changedField("email", null, staff.getEmail())
                                    .build());

                            log.info("Successfully audited LOGIN and updated lastLoginAt for staff member {}", staffId);
                        }
                    });
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid staffId format in principal: {}", principal.staffId());
                } catch (Exception e) {
                    log.error("Failed to process LOGIN audit event for staff: {}", principal.staffId(), e);
                }
            }
        }
    }
}
