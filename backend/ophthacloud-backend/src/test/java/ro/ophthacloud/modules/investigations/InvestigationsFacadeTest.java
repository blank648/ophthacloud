package ro.ophthacloud.modules.investigations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import ro.ophthacloud.infrastructure.persistence.BaseEntity;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;
import ro.ophthacloud.modules.investigations.dto.CreateInvestigationRequest;
import ro.ophthacloud.modules.investigations.dto.InvestigationDto;
import ro.ophthacloud.modules.investigations.dto.UpdateInvestigationResultRequest;
import ro.ophthacloud.modules.investigations.event.InvestigationResultAvailableEvent;
import ro.ophthacloud.modules.investigations.internal.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvestigationsFacade Test")
class InvestigationsFacadeTest {

    @Mock private InvestigationRepository investigationRepository;
    @Mock private InvestigationFileRepository investigationFileRepository;
    @Mock private MinioStorageService minioStorageService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private InvestigationsFacade facade;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ORDERED_BY_ID = UUID.randomUUID();
    private static final UUID INVESTIGATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        InvestigationService service = new InvestigationService(
                investigationRepository,
                investigationFileRepository,
                minioStorageService,
                eventPublisher
        );
        facade = new InvestigationsFacade(service);
    }

    @Test
    @DisplayName("createInvestigation: should create order with status ORDERED")
    void createInvestigation_shouldSetStatusOrdered() {
        CreateInvestigationRequest request = CreateInvestigationRequest.builder()
                .patientId(PATIENT_ID)
                .category(InvestigationCategoryType.OCT)
                .name("Macula OCT")
                .build();

        when(investigationRepository.save(any(InvestigationEntity.class))).thenAnswer(inv -> {
            InvestigationEntity entity = inv.getArgument(0);
            setId(entity, INVESTIGATION_ID);
            return entity;
        });

        InvestigationDto result = facade.createInvestigation(TENANT_ID, ORDERED_BY_ID, "Dr. Smith", request);

        assertThat(result.getStatus()).isEqualTo(InvestigationStatusType.ORDERED);
        verify(investigationRepository).save(any(InvestigationEntity.class));
    }

    @Test
    @DisplayName("updateInvestigationResult: should set status COMPLETED and publish event")
    void updateInvestigationResult_shouldCompleteAndPublishEvent() {
        InvestigationEntity entity = new InvestigationEntity();
        setId(entity, INVESTIGATION_ID);
        setTenantId(entity, TENANT_ID);
        entity.setPatientId(PATIENT_ID);
        entity.setCategory(InvestigationCategoryType.OCT);
        entity.setStatus(InvestigationStatusType.IN_PROGRESS);

        when(investigationRepository.findByIdAndTenantId(INVESTIGATION_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(investigationRepository.save(any())).thenReturn(entity);

        UpdateInvestigationResultRequest request = new UpdateInvestigationResultRequest();
        request.setStatus(InvestigationStatusType.COMPLETED);
        request.setInterpretation("Normal findings");

        InvestigationDto result = facade.updateInvestigationResult(TENANT_ID, INVESTIGATION_ID, request);

        assertThat(result.getStatus()).isEqualTo(InvestigationStatusType.COMPLETED);

        ArgumentCaptor<InvestigationResultAvailableEvent> captor = ArgumentCaptor.forClass(InvestigationResultAvailableEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        
        InvestigationResultAvailableEvent event = captor.getValue();
        assertThat(event.investigationId()).isEqualTo(INVESTIGATION_ID);
        assertThat(event.patientId()).isEqualTo(PATIENT_ID);
    }

    @Test
    @DisplayName("uploadFile: should reject file larger than 50MB")
    void uploadFile_shouldRejectLargeFile() {
        InvestigationEntity entity = new InvestigationEntity();
        setId(entity, INVESTIGATION_ID);
        setTenantId(entity, TENANT_ID);

        when(investigationRepository.findByIdAndTenantId(INVESTIGATION_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(investigationFileRepository.countByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(0L);

        // 51MB file
        byte[] content = new byte[0]; // mock will override size
        MockMultipartFile file = new MockMultipartFile("file", "test.dcm", "application/dicom", content) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024;
            }
        };

        assertThatThrownBy(() -> facade.uploadFile(TENANT_ID, INVESTIGATION_ID, file, "DICOM", "OD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds 50MB limit");
    }

    @Test
    @DisplayName("uploadFile: should reject upload if max 20 files reached")
    void uploadFile_shouldRejectIfMaxFilesReached() {
        InvestigationEntity entity = new InvestigationEntity();
        setId(entity, INVESTIGATION_ID);
        setTenantId(entity, TENANT_ID);

        when(investigationRepository.findByIdAndTenantId(INVESTIGATION_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(investigationFileRepository.countByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(20L); // 20 files already exist

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[100]);

        assertThatThrownBy(() -> facade.uploadFile(TENANT_ID, INVESTIGATION_ID, file, "IMAGE", "OS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum of 20 files allowed");
    }

    @Test
    @DisplayName("uploadFile: first file upload should auto-transition status ORDERED -> IN_PROGRESS")
    void uploadFile_shouldAutoTransitionToInProgress() {
        InvestigationEntity entity = new InvestigationEntity();
        setId(entity, INVESTIGATION_ID);
        setTenantId(entity, TENANT_ID);
        entity.setStatus(InvestigationStatusType.ORDERED); // currently ordered

        when(investigationRepository.findByIdAndTenantId(INVESTIGATION_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
        when(investigationFileRepository.countByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(0L);
        when(investigationFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[100]);

        facade.uploadFile(TENANT_ID, INVESTIGATION_ID, file, "IMAGE", "OD");

        assertThat(entity.getStatus()).isEqualTo(InvestigationStatusType.IN_PROGRESS);
        verify(investigationRepository).save(entity); // saves the updated status
    }

    @Test
    @DisplayName("listInvestigations: filtering by category returns correct subset")
    void listInvestigations_shouldFilterByCategory() {
        InvestigationEntity entity1 = new InvestigationEntity();
        setId(entity1, INVESTIGATION_ID);
        setTenantId(entity1, TENANT_ID);
        entity1.setCategory(InvestigationCategoryType.OCT);

        when(investigationRepository.findByTenantIdAndPatientIdAndCategory(
                eq(TENANT_ID), eq(PATIENT_ID), eq(InvestigationCategoryType.OCT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity1)));

        Page<InvestigationDto> result = facade.listInvestigations(TENANT_ID, PATIENT_ID, InvestigationCategoryType.OCT, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(InvestigationCategoryType.OCT);
    }

    private void setId(BaseEntity entity, UUID id) {
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setTenantId(TenantAwareEntity entity, UUID tenantId) {
        try {
            Field f = TenantAwareEntity.class.getDeclaredField("tenantId");
            f.setAccessible(true);
            f.set(entity, tenantId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
