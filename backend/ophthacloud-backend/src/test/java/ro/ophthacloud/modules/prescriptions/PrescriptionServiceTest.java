package ro.ophthacloud.modules.prescriptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.prescriptions.dto.CreatePrescriptionRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionDto;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionLineRequest;
import ro.ophthacloud.modules.prescriptions.event.PrescriptionSignedEvent;
import ro.ophthacloud.modules.prescriptions.internal.*;
import ro.ophthacloud.shared.security.ModulePermissions;
import ro.ophthacloud.shared.security.OphthaClinicalAuthenticationToken;
import ro.ophthacloud.shared.security.OphthaPrincipal;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PrescriptionService}.
 * No Spring context — pure Mockito.
 * Covers OC-023 acceptance criteria:
 * create, sign (supersede + event), cancel (409 on double cancel), SEQ computation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PrescriptionService")
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository        prescriptionRepository;
    @Mock private PrescriptionLineRepository    prescriptionLineRepository;
    @Mock private PrescriptionNumberGenerator   numberGenerator;
    @Mock private ApplicationEventPublisher     eventPublisher;
    @Mock private JdbcTemplate                  jdbcTemplate;

    @InjectMocks
    private PrescriptionService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STAFF_ID   = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID RX_ID      = UUID.randomUUID();

    @BeforeEach
    void setUpContext() {
        TenantContext.set(TENANT_ID);
        OphthaPrincipal principal = new OphthaPrincipal(
                STAFF_ID.toString(),
                TENANT_ID.toString(),
                STAFF_ID.toString(),
                "DOCTOR",
                Map.of("prescriptions", new ModulePermissions(true, true, true, true, true, false))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new OphthaClinicalAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Test 1: createPrescription — saves entity + lines ────────────────────

    @Test
    @DisplayName("createPrescription: should save entity, save lines, and return DTO with correct prescriptionNumber")
    void createPrescription_shouldSaveAndReturnDto() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any()))
                .thenReturn("OC-004821");
        when(numberGenerator.generate(eq("OC-004821"), eq(TENANT_ID), any()))
                .thenReturn("RX-2026-004821");
        when(prescriptionRepository.save(any())).thenAnswer(inv -> {
            PrescriptionEntity e = inv.getArgument(0);
            setId(e, RX_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(prescriptionLineRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any()))
                .thenReturn(List.of());

        PrescriptionDto result = service.createPrescription(
                TENANT_ID, STAFF_ID, "Dr. Test", baseRequest());

        assertThat(result).isNotNull();
        assertThat(result.getPrescriptionNumber()).isEqualTo("RX-2026-004821");
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        verify(prescriptionRepository).save(any());
        verify(prescriptionLineRepository).saveAll(any());
    }

    // ── Test 2: createPrescription — QR token is UUID only ───────────────────

    @Test
    @DisplayName("createPrescription: qrCodeToken must be a valid UUID string with no extra suffix")
    void createPrescription_qrCodeToken_isUuidOnly() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any()))
                .thenReturn("OC-004821");
        when(numberGenerator.generate(any(), any(), any())).thenReturn("RX-2026-004821");
        when(prescriptionRepository.save(any())).thenAnswer(inv -> {
            PrescriptionEntity e = inv.getArgument(0);
            setId(e, RX_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(prescriptionLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any())).thenReturn(List.of());

        PrescriptionDto result = service.createPrescription(
                TENANT_ID, STAFF_ID, "Dr. Test", baseRequest());

        String token = result.getQrCodeToken();
        assertThat(token).isNotNull();
        // Must be a plain UUID (36 chars: 8-4-4-4-12) with no timestamp or extra characters
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    // ── Test 3: createPrescription — lines auto-compute SEQ ──────────────────

    @Test
    @DisplayName("createPrescription: prescription lines should have SEQ auto-computed (Sph + Cyl/2, rounded to 0.25)")
    void createPrescription_lines_shouldAutoComputeSeq() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any(), any()))
                .thenReturn("OC-000001");
        when(numberGenerator.generate(any(), any(), any())).thenReturn("RX-2026-000001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PrescriptionLineEntity>> linesCaptor = ArgumentCaptor.forClass(List.class);

        when(prescriptionRepository.save(any())).thenAnswer(inv -> {
            PrescriptionEntity e = inv.getArgument(0);
            setId(e, RX_ID);
            setTenantId(e, TENANT_ID);
            return e;
        });
        when(prescriptionLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any())).thenReturn(List.of());

        // Request: OD Sph=-2.50, Cyl=-0.75 → SEQ = -2.50 + (-0.375) = -2.875 → rounds to -2.75
        CreatePrescriptionRequest req = baseRequestWithLine("-2.50", "-0.75");
        service.createPrescription(TENANT_ID, STAFF_ID, "Dr. Test", req);

        verify(prescriptionLineRepository).saveAll(linesCaptor.capture());
        List<PrescriptionLineEntity> savedLines = linesCaptor.getValue();
        assertThat(savedLines).hasSize(1);
        // SEQ = -2.50 + (-0.375) = -2.875 → nearest 0.25 = -2.75
        assertThat(savedLines.get(0).getSeq()).isEqualByComparingTo(new BigDecimal("-2.75"));
    }

    // ── Test 4: signPrescription — sets signedAt and publishes event ─────────

    @Test
    @DisplayName("signPrescription: should set signedAt and publish PrescriptionSignedEvent")
    void signPrescription_shouldSetSignedAtAndPublishEvent() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.ACTIVE);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(prescriptionRepository.supersedePreviousActive(any(), any(), any(), any()))
                .thenReturn(0);
        when(prescriptionRepository.save(any())).thenReturn(entity);
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any()))
                .thenReturn(List.of());

        service.signPrescription(TENANT_ID, RX_ID);

        assertThat(entity.getSignedAt()).isNotNull();

        ArgumentCaptor<PrescriptionSignedEvent> captor =
                ArgumentCaptor.forClass(PrescriptionSignedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        PrescriptionSignedEvent event = captor.getValue();
        assertThat(event.prescriptionId()).isEqualTo(RX_ID);
        assertThat(event.patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
    }

    // ── Test 5: signPrescription — supersedes previous ACTIVE of same type ───

    @Test
    @DisplayName("signPrescription: should call supersedePreviousActive for the same prescription type")
    void signPrescription_shouldCallSupersedePreviousActive() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.ACTIVE);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(prescriptionRepository.supersedePreviousActive(
                eq(PATIENT_ID), eq(TENANT_ID), eq(PrescriptionType.DISTANCE), eq(RX_ID)))
                .thenReturn(1);
        when(prescriptionRepository.save(any())).thenReturn(entity);
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any()))
                .thenReturn(List.of());

        service.signPrescription(TENANT_ID, RX_ID);

        verify(prescriptionRepository).supersedePreviousActive(
                PATIENT_ID, TENANT_ID, PrescriptionType.DISTANCE, RX_ID);
    }

    // ── Test 6: cancelPrescription — sets status CANCELLED ───────────────────

    @Test
    @DisplayName("cancelPrescription: should set status to CANCELLED")
    void cancelPrescription_shouldSetStatusCancelled() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.ACTIVE);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(prescriptionRepository.save(any())).thenReturn(entity);
        when(prescriptionLineRepository.findByTenantIdAndPrescriptionId(any(), any()))
                .thenReturn(List.of());

        PrescriptionDto result = service.cancelPrescription(TENANT_ID, RX_ID);

        assertThat(result.getStatus()).isEqualTo(PrescriptionStatusType.CANCELLED);
        assertThat(entity.getStatus()).isEqualTo(PrescriptionStatusType.CANCELLED);
    }

    // ── Test 7: cancelPrescription — 409 if already CANCELLED ─────────────────

    @Test
    @DisplayName("cancelPrescription: should throw PrescriptionAlreadyCancelledException when already CANCELLED")
    void cancelPrescription_shouldThrow_whenAlreadyCancelled() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.CANCELLED);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.cancelPrescription(TENANT_ID, RX_ID))
                .isInstanceOf(PrescriptionAlreadyCancelledException.class);
    }

    // ── Test 8: cancelPrescription — 409 if EXPIRED ───────────────────────────

    @Test
    @DisplayName("cancelPrescription: should throw PrescriptionAlreadyCancelledException when EXPIRED")
    void cancelPrescription_shouldThrow_whenExpired() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.EXPIRED);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.cancelPrescription(TENANT_ID, RX_ID))
                .isInstanceOf(PrescriptionAlreadyCancelledException.class);
    }

    // ── Test 9: getPrescription — throws 404 when not found ─────────────────

    @Test
    @DisplayName("getPrescription: should throw PrescriptionNotFoundException when not found")
    void getPrescription_shouldThrow_whenNotFound() {
        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPrescription(TENANT_ID, RX_ID))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    // ── Test 10: signPrescription — 409 on CANCELLED prescription ────────────

    @Test
    @DisplayName("signPrescription: should throw PrescriptionAlreadyCancelledException when prescription is CANCELLED")
    void signPrescription_shouldThrow_whenCancelled() {
        PrescriptionEntity entity = prescriptionEntity(PrescriptionStatusType.CANCELLED);

        when(prescriptionRepository.findByIdAndTenantId(RX_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.signPrescription(TENANT_ID, RX_ID))
                .isInstanceOf(PrescriptionAlreadyCancelledException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatePrescriptionRequest baseRequest() {
        return CreatePrescriptionRequest.builder()
                .patientId(PATIENT_ID)
                .prescriptionType(PrescriptionType.DISTANCE)
                .validFrom(LocalDate.now())
                .validUntil(LocalDate.now().plusYears(1))
                .lines(List.of(
                        PrescriptionLineRequest.builder()
                                .eye("OD")
                                .sph(new BigDecimal("-2.50"))
                                .cyl(new BigDecimal("-0.75"))
                                .axis((short) 90)
                                .vaCc("6/6")
                                .build()
                ))
                .build();
    }

    private CreatePrescriptionRequest baseRequestWithLine(String sph, String cyl) {
        return CreatePrescriptionRequest.builder()
                .patientId(PATIENT_ID)
                .prescriptionType(PrescriptionType.DISTANCE)
                .validFrom(LocalDate.now())
                .validUntil(LocalDate.now().plusYears(1))
                .lines(List.of(
                        PrescriptionLineRequest.builder()
                                .eye("OD")
                                .sph(new BigDecimal(sph))
                                .cyl(new BigDecimal(cyl))
                                .axis((short) 90)
                                .build()
                ))
                .build();
    }

    private PrescriptionEntity prescriptionEntity(PrescriptionStatusType status) {
        PrescriptionEntity e = new PrescriptionEntity();
        setId(e, RX_ID);
        setTenantId(e, TENANT_ID);
        e.setPatientId(PATIENT_ID);
        e.setPrescriptionType(PrescriptionType.DISTANCE);
        e.setStatus(status);
        e.setPrescriptionNumber("RX-2026-004821");
        e.setIssuedById(STAFF_ID);
        e.setIssuedByName("Dr. Test");
        e.setValidFrom(LocalDate.now());
        e.setValidUntil(LocalDate.now().plusYears(1));
        return e;
    }

    private void setId(Object entity, UUID id) {
        setField(entity, BaseEntity.class, "id", id);
    }

    private void setTenantId(Object entity, UUID tenantId) {
        setField(entity, TenantAwareEntity.class, "tenantId", tenantId);
    }

    private void setField(Object target, Class<?> declaringClass, String name, Object value) {
        try {
            Field f = declaringClass.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + name + "' via reflection", e);
        }
    }
}
