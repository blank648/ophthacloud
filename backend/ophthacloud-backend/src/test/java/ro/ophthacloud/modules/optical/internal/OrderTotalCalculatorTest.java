package ro.ophthacloud.modules.optical.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderTotalCalculator}.
 * Verifies: total = Σ(unitPrice × quantity × (1 - discountPercent/100)) − deposit
 * Internal precision is 4 decimal places; DTO-layer rounds to 2.
 */
@DisplayName("OrderTotalCalculator")
class OrderTotalCalculatorTest {

    private final OrderTotalCalculator calculator = new OrderTotalCalculator();

    // ── Test 1: Canonical example from spec ──────────────────────────────────
    // (100.00×2×0.90) + (50.00×1×1.00) - 20.00 = 210.00

    @Test
    @DisplayName("calculateTotal: (100×2×0.90) + (50×1×1.00) - 20.00 deposit = 210.00")
    void calculateTotal_canonicalExample() {
        OpticalOrderItemEntity item1 = itemWithPrice("100.00", 2, "10.00"); // 10% discount
        OpticalOrderItemEntity item2 = itemWithPrice("50.00",  1, "0.00");  // no discount

        BigDecimal total = calculator.calculateTotal(List.of(item1, item2), new BigDecimal("20.00"));

        // Expect 210.0000 internally (4 scale)
        assertThat(total.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("210.00"));
    }

    // ── Test 2: Empty item list ────────────────────────────────────────────────

    @Test
    @DisplayName("calculateTotal: empty item list with deposit returns negative deposit")
    void calculateTotal_emptyItems() {
        BigDecimal total = calculator.calculateTotal(List.of(), new BigDecimal("20.00"));

        assertThat(total.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("-20.00"));
    }

    // ── Test 3: No deposit (null) ─────────────────────────────────────────────

    @Test
    @DisplayName("calculateTotal: null deposit treated as zero")
    void calculateTotal_nullDeposit() {
        OpticalOrderItemEntity item = itemWithPrice("100.00", 1, "0.00");

        BigDecimal total = calculator.calculateTotal(List.of(item), null);

        assertThat(total.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // ── Test 4: 100% discount on an item ──────────────────────────────────────

    @Test
    @DisplayName("calculateTotal: 100% discount results in zero line contribution")
    void calculateTotal_fullDiscount() {
        OpticalOrderItemEntity item1 = itemWithPrice("200.00", 1, "100.00"); // 100% discount → 0
        OpticalOrderItemEntity item2 = itemWithPrice("50.00",  1, "0.00");

        BigDecimal total = calculator.calculateTotal(List.of(item1, item2), BigDecimal.ZERO);

        assertThat(total.setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // ── Test 5: Precision — fractional amounts do not overflow ────────────────

    @Test
    @DisplayName("calculateTotal: fractional prices are computed at 4-decimal precision")
    void calculateTotal_fractionalPrices() {
        OpticalOrderItemEntity item = itemWithPrice("33.33", 3, "0.00"); // 33.33 × 3 = 99.99

        BigDecimal total = calculator.calculateTotal(List.of(item), BigDecimal.ZERO);

        assertThat(total.setScale(4, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("99.9900"));
    }

    // ── Test 6: Line total is set on item by calculator ──────────────────────

    @Test
    @DisplayName("calculateTotal: item.lineTotal is updated by the calculator")
    void calculateTotal_setsLineTotalOnItem() {
        OpticalOrderItemEntity item = itemWithPrice("100.00", 2, "10.00"); // 100×2×0.90 = 180

        calculator.calculateTotal(List.of(item), BigDecimal.ZERO);

        assertThat(item.getLineTotal().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("180.00"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OpticalOrderItemEntity itemWithPrice(String unitPrice, int quantity, String discountPercent) {
        OpticalOrderItemEntity item = new OpticalOrderItemEntity();
        item.setDescription("Test item");
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        item.setDiscountPercent(new BigDecimal(discountPercent));
        return item;
    }
}
