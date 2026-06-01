package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.optical.dto.ServiceCatalogDto;
import ro.ophthacloud.shared.tenant.TenantContext;
import ro.ophthacloud.shared.api.ApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/optical/services")
@RequiredArgsConstructor
public class ServiceCatalogController {

    private final ServiceCatalogRepository serviceCatalogRepository;

    @GetMapping
    @PreAuthorize("hasPermission('optical', 'MODULE', 'VIEW')")
    public ApiResponse<List<ServiceCatalogDto>> listServices() {
        UUID tenantId = TenantContext.require();
        List<ServiceCatalogEntity> items = serviceCatalogRepository.findByTenantIdAndIsActiveTrue(tenantId);
        
        // Auto-seed typical clinic catalog items if empty
        if (items.isEmpty()) {
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Consultație inițială oftalmolog", "medical", new BigDecimal("200.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Consultație follow-up oftalmolog", "medical", new BigDecimal("100.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Consultație optometrist", "medical", new BigDecimal("80.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("OCT macular", "investigation", new BigDecimal("150.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Câmp vizual", "investigation", new BigDecimal("80.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Topografie corneană", "investigation", new BigDecimal("60.00"), new BigDecimal("19.00"), "RON"));
            serviceCatalogRepository.save(ServiceCatalogEntity.create("Consiliere optică", "optical", new BigDecimal("40.00"), new BigDecimal("19.00"), "RON"));
            
            items = serviceCatalogRepository.findByTenantIdAndIsActiveTrue(tenantId);
        }
        
        return ApiResponse.of(items.stream().map(ServiceCatalogDto::from).toList());
    }
}
