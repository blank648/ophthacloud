package ro.ophthacloud.modules.admin.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.admin.dto.CreateEquipmentRequest;
import ro.ophthacloud.modules.admin.dto.EquipmentDto;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public List<EquipmentDto> listEquipment() {
        UUID tenantId = TenantContext.require();
        return equipmentRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(EquipmentDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public EquipmentDto createEquipment(CreateEquipmentRequest request) {
        UUID tenantId = TenantContext.require();

        EquipmentEntity entity = EquipmentEntity.builder()
                .name(request.name())
                .brand(request.brand())
                .type(request.type())
                .location(request.location())
                .dicomEnabled(request.dicomEnabled())
                .build();

        entity = equipmentRepository.save(entity);
        log.info("Created equipment {} for tenant {}", entity.getId(), tenantId);
        return EquipmentDto.from(entity);
    }

    @Transactional
    public void deleteEquipment(UUID id) {
        UUID tenantId = TenantContext.require();
        EquipmentEntity entity = equipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
        
        if (!entity.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Equipment not found");
        }

        equipmentRepository.delete(entity);
        log.info("Deleted equipment {} for tenant {}", id, tenantId);
    }
}
