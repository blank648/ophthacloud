package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "optical_order_items")
@Getter
@Setter
@NoArgsConstructor
public class OpticalOrderItemEntity extends TenantAwareEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "service_item_id")
    private UUID serviceItemId;

    @Column(name = "stock_item_id")
    private UUID stockItemId;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "eye", length = 2)
    private String eye;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lens_specifications", columnDefinition = "jsonb")
    private String lensSpecifications;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
