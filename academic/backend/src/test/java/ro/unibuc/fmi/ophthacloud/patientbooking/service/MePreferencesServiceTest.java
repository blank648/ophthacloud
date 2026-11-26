package ro.unibuc.fmi.ophthacloud.patientbooking.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import ro.unibuc.fmi.ophthacloud.patientbooking.dto.PreferencesRequest;
import ro.unibuc.fmi.ophthacloud.patientbooking.entity.PatientEntity;
import ro.unibuc.fmi.ophthacloud.patientbooking.repository.PatientRepository;
import ro.unibuc.fmi.ophthacloud.securityaudit.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import jakarta.validation.ConstraintViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MePreferencesServiceTest {
    private final UUID id = UUID.randomUUID();
    private final CurrentUserProvider current = mock(CurrentUserProvider.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final MePreferencesService service = new MePreferencesService(current, patients);

    private void signIn(UserRole role) {
        when(current.currentUser()).thenReturn(new AuthenticatedUser(id, "user@example.com", role));
    }

    @Test
    void readsAndUpdatesAuthenticatedPatient() {
        signIn(UserRole.PATIENT);
        var patient = new PatientEntity();
        UUID patientId = UUID.randomUUID();
        ReflectionTestUtils.setField(patient, "id", patientId);
        patient.updatePreferences(new BigDecimal("1.0"), false);
        when(patients.findByUserId(id)).thenReturn(Optional.of(patient));
        when(patients.findLockedByUserId(id)).thenReturn(Optional.of(patient));
        assertThat(service.me().patientId()).isEqualTo(patientId);
        assertThat(service.me().uiFontScale()).isEqualByComparingTo("1.0");

        var updated = service.update(new PreferencesRequest(new BigDecimal("1.5"), true));
        assertThat(updated.highContrast()).isTrue();
        assertThat(updated.uiFontScale()).isEqualByComparingTo("1.5");
        assertThat(service.me()).isEqualTo(updated);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DOCTOR", "ADMIN", "RECEPTIONIST"})
    void staffReceiveDefaults(UserRole role) {
        signIn(role);
        var result = service.me();
        assertThat(result.patientId()).isNull();
        assertThat(result.uiFontScale()).isEqualByComparingTo("1.0");
        assertThat(result.highContrast()).isFalse();
        verifyNoInteractions(patients);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "2.1", "1.25"})
    void validatesScaleBeforeUpdating(String scale) {
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new MethodValidationInterceptor());
        var validated = (MePreferencesService) proxy.getProxy();
        assertThatThrownBy(() -> validated.update(new PreferencesRequest(new BigDecimal(scale), true)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(patients);
    }
}
