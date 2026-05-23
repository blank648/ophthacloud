package ro.ophthacloud.modules.admin.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import ro.ophthacloud.shared.audit.AuditLogService;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginEventListener unit tests")
class LoginEventListenerTest {

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LoginEventListener listener;

    @Test
    @DisplayName("onAuthenticationSuccess: should update lastLoginAt and log LOGIN when user has not logged in recently")
    void onAuthenticationSuccess_shouldUpdateAndLog() {
        UUID staffId = UUID.randomUUID();
        OphthaPrincipal principal = new OphthaPrincipal(
                "subject",
                UUID.randomUUID().toString(),
                staffId.toString(),
                null,
                "DOCTOR",
                java.util.Map.of()
        );
        OphthaClinicalAuthenticationToken token = new OphthaClinicalAuthenticationToken(principal, null, java.util.List.of());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(token);

        StaffMemberEntity staff = new StaffMemberEntity();
        staff.setFirstName("John");
        staff.setLastName("Doe");
        staff.setEmail("john.doe@clinic.com");
        staff.setRole(StaffRole.DOCTOR);
        staff.setIsActive(true);

        when(staffMemberRepository.findById(staffId)).thenReturn(Optional.of(staff));

        listener.onAuthenticationSuccess(event);

        assertThat(staff.getLastLoginAt()).isNotNull();
        verify(staffMemberRepository).save(staff);
        verify(auditLogService).log(any());
    }

    @Test
    @DisplayName("onAuthenticationSuccess: should NOT update or log when user has logged in recently")
    void onAuthenticationSuccess_shouldNotUpdateOrLog_whenRecent() {
        UUID staffId = UUID.randomUUID();
        OphthaPrincipal principal = new OphthaPrincipal(
                "subject",
                UUID.randomUUID().toString(),
                staffId.toString(),
                null,
                "DOCTOR",
                java.util.Map.of()
        );
        OphthaClinicalAuthenticationToken token = new OphthaClinicalAuthenticationToken(principal, null, java.util.List.of());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(token);

        StaffMemberEntity staff = new StaffMemberEntity();
        staff.setFirstName("John");
        staff.setLastName("Doe");
        staff.setEmail("john.doe@clinic.com");
        staff.setRole(StaffRole.DOCTOR);
        staff.setIsActive(true);
        staff.setLastLoginAt(Instant.now().minus(2, ChronoUnit.MINUTES));

        when(staffMemberRepository.findById(staffId)).thenReturn(Optional.of(staff));

        listener.onAuthenticationSuccess(event);

        verify(staffMemberRepository, never()).save(any());
        verify(auditLogService, never()).log(any());
    }
}
