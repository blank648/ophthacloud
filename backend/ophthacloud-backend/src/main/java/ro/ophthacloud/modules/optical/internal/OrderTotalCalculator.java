package ro.ophthacloud.modules.optical.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderTotalCalculator {

    /**
     * Order total = Σ(unitPrice × quantity × (1 - discountPercent/100)) − deposit
     * Computation happens at higher precision internally (scale 4) per GUIDE_06 §6.5.
     */
    public BigDecimal calculateTotal(List<OpticalOrderItemEntity> items, BigDecimal depositPaid) {
        BigDecimal subtotal = items.stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                    BigDecimal discountPercent = item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO;

                    BigDecimal lineTotalBeforeDiscount = unitPrice.multiply(quantity);
                    BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                            discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    );

                    BigDecimal finalLineTotal = lineTotalBeforeDiscount.multiply(discountMultiplier);
                    
                    // Update line total on entity directly if needed, or caller updates it
                    item.setLineTotal(finalLineTotal.setScale(4, RoundingMode.HALF_UP));
                    return finalLineTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal effectiveDeposit = depositPaid != null ? depositPaid : BigDecimal.ZERO;
        return subtotal.subtract(effectiveDeposit).setScale(4, RoundingMode.HALF_UP);
    }
}
