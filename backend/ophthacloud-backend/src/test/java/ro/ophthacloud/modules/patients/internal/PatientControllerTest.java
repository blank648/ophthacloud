package ro.ophthacloud.modules.patients.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.ophthacloud.modules.patients.PatientManagementFacade;
import ro.ophthacloud.modules.patients.dto.CreatePatientRequest;
import ro.ophthacloud.modules.patients.dto.PatientDto;
import ro.ophthacloud.modules.patients.dto.PatientSummaryDto;
import ro.ophthacloud.shared.api.GlobalExceptionHandler;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientController")
class PatientControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PatientManagementFacade patientManagementFacade;

    @InjectMocks
    private PatientController patientController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("listPatients: returns paginated response")
    void listPatients_shouldReturnPage() throws Exception {
        PatientSummaryDto summary = new PatientSummaryDto(UUID.randomUUID(), "OC-1", "A", "B", LocalDate.now().minusYears(30), 30, GenderType.MALE, "123", "a@b.com", false, true, List.of(), Instant.now());
        when(patientManagementFacade.listPatients(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/patients")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mrn").value("OC-1"));
    }

    @Test
    @DisplayName("createPatient: returns 201 on success")
    void createPatient_shouldReturn201() throws Exception {
        CreatePatientRequest req = new CreatePatientRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setDateOfBirth(LocalDate.of(1990, 1, 1));
        req.setGender(GenderType.MALE);
        req.setPhone("0755123123");

        PatientDto dto = new PatientDto(UUID.randomUUID(), "OC-2", "John", "Doe", req.getDateOfBirth(), 30, GenderType.MALE, "1234567890123", req.getPhone(), null, "a@b.com", null, null, null, null, null, null, null, null, null, null, null, false, null, true, null, null, null, Instant.now(), null);
        when(patientManagementFacade.createPatient(any(CreatePatientRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    @DisplayName("createPatient: returns 400 on validation failure")
    void createPatient_shouldReturn400_whenInvalid() throws Exception {
        CreatePatientRequest req = new CreatePatientRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("deletePatient: returns 204")
    void deletePatient_shouldReturn204() throws Exception {
        UUID patientId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNoContent());

        verify(patientManagementFacade).deletePatient(patientId);
    }
}
