package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
public class StockItemEntity extends TenantAwareEntity {

    @Column(name = "service_item_id")
    private UUID serviceItemId;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "barcode", length = 128)
    private String barcode;

    @Column(name = "brand", length = 128)
    private String brand;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock = 5;

    @Column(name = "unit_cost", precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "unit_price", precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "RON";

    @Column(name = "location", length = 128)
    private String location;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;
}
