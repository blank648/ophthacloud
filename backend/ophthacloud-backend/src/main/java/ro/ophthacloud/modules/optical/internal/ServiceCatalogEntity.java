package ro.ophthacloud.modules.optical.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ro.ophthacloud.infrastructure.persistence.TenantAwareEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "service_catalog")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {}, callSuper = true)
public class ServiceCatalogEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "brand", length = 128)
    private String brand;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public static ServiceCatalogEntity create(String name, String category, BigDecimal unitPrice, BigDecimal vatRate, String currency) {
        ServiceCatalogEntity entity = new ServiceCatalogEntity();
        entity.setName(name);
        entity.setCategory(category);
        entity.setUnitPrice(unitPrice);
        entity.setVatRate(vatRate);
        entity.setCurrency(currency);
        entity.setIsActive(true);
        return entity;
    }
}
