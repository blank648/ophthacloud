package ro.ophthacloud.modules.optical.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.optical.event.LowStockAlertEvent;
import ro.ophthacloud.modules.optical.dto.StockItemDto;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockItemRepository stockItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<StockItemDto> listItems(UUID tenantId) {
        return stockItemRepository.findAll().stream()
                .filter(s -> s.getTenantId().equals(tenantId))
                .map(StockItemDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockItemDto> getLowStockReport(UUID tenantId) {
        return stockItemRepository.findByTenantIdAndCurrentStockLessThanEqual(tenantId, 5).stream() // Default threshold
                .map(StockItemDto::from)
                .toList();
    }

    @Transactional
    public StockItemDto updateStockLevel(UUID tenantId, UUID stockItemId, int newStock) {
        StockItemEntity item = findStockItemOrThrow(tenantId, stockItemId);
        item.setCurrentStock(newStock);
        item.setLastRestockedAt(java.time.Instant.now());
        
        StockItemEntity saved = stockItemRepository.save(item);
        checkAndPublishLowStockAlert(saved);
        
        return StockItemDto.from(saved);
    }

    private void checkAndPublishLowStockAlert(StockItemEntity item) {
        if (item.getCurrentStock() <= item.getMinimumStock()) {
            eventPublisher.publishEvent(new LowStockAlertEvent(
                    item.getId(),
                    item.getTenantId(),
                    item.getName(),
                    item.getSku(),
                    item.getCurrentStock(),
                    item.getMinimumStock()
            ));
        }
    }

    private StockItemEntity findStockItemOrThrow(UUID tenantId, UUID stockItemId) {
        return stockItemRepository.findById(stockItemId)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new OpticalDomainException("STOCK_ITEM_NOT_FOUND", "Stock item not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void deductStock(UUID tenantId, UUID stockItemId, int quantity) {
        if (stockItemId == null || quantity <= 0) {
            return;
        }

        StockItemEntity item = stockItemRepository.findById(stockItemId)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new OpticalDomainException("STOCK_ITEM_NOT_FOUND", "Stock item not found", org.springframework.http.HttpStatus.NOT_FOUND));

        if (item.getCurrentStock() < quantity) {
            throw new OpticalDomainException("INSUFFICIENT_STOCK", "Not enough stock for item " + item.getName(), org.springframework.http.HttpStatus.CONFLICT);
        }

        item.setCurrentStock(item.getCurrentStock() - quantity);
        StockItemEntity saved = stockItemRepository.save(item);

        checkAndPublishLowStockAlert(saved);
    }
}
