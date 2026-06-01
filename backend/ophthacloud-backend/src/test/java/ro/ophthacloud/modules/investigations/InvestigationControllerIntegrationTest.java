package ro.ophthacloud.modules.investigations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ro.ophthacloud.modules.investigations.dto.CreateInvestigationRequest;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.investigations.dto.InvestigationFileDto;
import ro.ophthacloud.modules.investigations.dto.UpdateInvestigationResultRequest;
import ro.ophthacloud.shared.api.ApiResponse;
import ro.ophthacloud.shared.api.PagedApiResponse;
import ro.ophthacloud.shared.test.BaseIntegrationTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.ophthacloud.modules.investigations.internal.MinioStorageService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvestigationController Integration")
class InvestigationControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders doctorHeaders;
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @MockitoBean
    private MinioStorageService minioStorageService;
    @BeforeEach
    void setUpData() {
        ensureTenantExists(TENANT_ID);
        ensurePatientExists(PATIENT_ID);
        doctorHeaders = headersForRole("DOCTOR", TENANT_ID);
    }

    private void ensurePatientExists(UUID patientId) {
        jdbcTemplate.update("""
                INSERT INTO patients (id, tenant_id, mrn, first_name, last_name, date_of_birth, gender)
                VALUES (?, ?, ?, ?, ?, '1980-01-01', 'MALE')
                ON CONFLICT (id) DO NOTHING
                """,
                patientId,
                TENANT_ID,
                "MRN-" + patientId.toString().substring(0, 8),
                "John",
                "Doe"
        );
    }

    @Test
    @DisplayName("POST /api/v1/investigations: should return 201 and create order")
    void createInvestigation_shouldReturn201() {
        CreateInvestigationRequest request = CreateInvestigationRequest.builder()
                .patientId(PATIENT_ID)
                .category(InvestigationCategoryType.OCT)
                .name("Macula OCT")
                .isUrgent(true)
                .build();

        ApiResponse<InvestigationDto> response = client.post()
                .uri("/api/v1/investigations")
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(response).isNotNull();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getStatus()).isEqualTo(InvestigationStatusType.ORDERED);
        assertThat(response.data().getCategory()).isEqualTo(InvestigationCategoryType.OCT);
        assertThat(response.data().getIsUrgent()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/investigations/{id}/files: should upload multipart and return 201")
    void uploadFile_shouldReturn201() {
        // 1. Create order
        CreateInvestigationRequest request = CreateInvestigationRequest.builder()
                .patientId(PATIENT_ID)
                .category(InvestigationCategoryType.FUNDUS_PHOTO)
                .name("Fundus OD")
                .build();

        ApiResponse<InvestigationDto> createResp = client.post()
                .uri("/api/v1/investigations")
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
                
        UUID id = createResp.data().getId();

        // 2. Upload file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "test-fundus.png";
            }
        };
        body.add("file", fileResource);
        body.add("fileType", "IMAGE");
        body.add("laterality", "OD");

        ApiResponse<InvestigationFileDto> uploadResp = client.post()
                .uri("/api/v1/investigations/{id}/files", id)
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(uploadResp).isNotNull();
        assertThat(uploadResp.data().getFileName()).isEqualTo("test-fundus.png");
        assertThat(uploadResp.data().getFileType()).isEqualTo("IMAGE");
    }

    @Test
    @DisplayName("PUT /api/v1/investigations/{id}: should update result and return 200")
    void updateInvestigation_shouldReturn200() {
        // 1. Create order
        CreateInvestigationRequest request = CreateInvestigationRequest.builder()
                .patientId(PATIENT_ID)
                .category(InvestigationCategoryType.VISUAL_FIELD)
                .name("VF 24-2")
                .build();

        ApiResponse<InvestigationDto> createResp = client.post()
                .uri("/api/v1/investigations")
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
                
        UUID id = createResp.data().getId();

        // 2. Update result
        UpdateInvestigationResultRequest updateReq = new UpdateInvestigationResultRequest();
        updateReq.setStatus(InvestigationStatusType.COMPLETED);
        updateReq.setInterpretation("No defects");

        ApiResponse<InvestigationDto> updateResp = client.put()
                .uri("/api/v1/investigations/{id}", id)
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateReq)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(updateResp).isNotNull();
        assertThat(updateResp.data().getStatus()).isEqualTo(InvestigationStatusType.COMPLETED);
        assertThat(updateResp.data().getInterpretation()).isEqualTo("No defects");
    }

    @Test
    @DisplayName("GET /api/v1/investigations: should filter by category")
    void listInvestigations_shouldFilterByCategory() {
        // 1. Create OCT
        client.post()
                .uri("/api/v1/investigations")
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(CreateInvestigationRequest.builder()
                        .patientId(PATIENT_ID).category(InvestigationCategoryType.OCT).name("OCT").build())
                .retrieve().toBodilessEntity();

        // 2. Create BIOMETRY
        client.post()
                .uri("/api/v1/investigations")
                .headers(h -> h.putAll(doctorHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .body(CreateInvestigationRequest.builder()
                        .patientId(PATIENT_ID).category(InvestigationCategoryType.BIOMETRY).name("IOL Master").build())
                .retrieve().toBodilessEntity();

        // 3. List only OCT
        PagedApiResponse<InvestigationDto> listResp = client.get()
                .uri(builder -> builder.path("/api/v1/investigations")
                        .queryParam("patientId", PATIENT_ID)
                        .queryParam("category", "OCT")
                        .build())
                .headers(h -> h.putAll(doctorHeaders))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(listResp).isNotNull();
        assertThat(listResp.data()).isNotEmpty();
        // Should only contain OCT
        listResp.data().forEach(inv -> 
            assertThat(inv.getCategory()).isEqualTo(InvestigationCategoryType.OCT)
        );
    }
}
