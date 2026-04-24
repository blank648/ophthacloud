package ro.ophthacloud.modules.emr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ro.ophthacloud.modules.emr.internal.SeqCalculator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SeqCalculator}.
 * <p>
 * Covers OC-020 acceptance criteria (≥ 6 tests):
 * All cases from GUIDE_06 §3.2 — SEQ = Sph + Cyl/2, rounded to nearest 0.25D.
 */
@DisplayName("SeqCalculator")
class SeqCalculatorTest {

    private final SeqCalculator calculator = new SeqCalculator();

    // ── Test 1: SEQ(-2.50, -0.75) = -2.75  ───────────────────────────────────
    // raw = -2.50 + (-0.75/2) = -2.875 → round(-2.875 * 4) / 4 = round(-11.5) / 4 = -12/4 = -3.0
    // Note: Java Math.round uses "round half up", so -11.5 rounds to -11, giving -2.75
    // Math.round(-11.5) = -11 in Java (rounds towards positive infinity at 0.5)

    @Test
    @DisplayName("compute: should return -2.75 for Sph=-2.50, Cyl=-0.75")
    void compute_shouldReturn_minus2point75_forMinus250Minus075() {
        // raw = -2.50 + (-0.375) = -2.875; round(-2.875 * 4.0) / 4.0 = round(-11.5) / 4.0 = -11/4.0 = -2.75
        Double result = calculator.compute(-2.50, -0.75);
        assertThat(result).isEqualTo(-2.75);
    }

    // ── Test 2: SEQ(+3.00, -1.50) = +2.25 ───────────────────────────────────

    @Test
    @DisplayName("compute: should return +2.25 for Sph=+3.00, Cyl=-1.50")
    void compute_shouldReturn_plus2point25_forPlus300MinuS150() {
        // raw = 3.00 + (-0.75) = 2.25; already on 0.25 step → 2.25
        Double result = calculator.compute(3.00, -1.50);
        assertThat(result).isEqualTo(2.25);
    }

    // ── Test 3: SEQ(0.0, 0.0) = 0.0 ──────────────────────────────────────────

    @Test
    @DisplayName("compute: should return 0.0 for Sph=0.0, Cyl=0.0")
    void compute_shouldReturn_zero_forZeroInputs() {
        Double result = calculator.compute(0.0, 0.0);
        assertThat(result).isEqualTo(0.0);
    }

    // ── Test 4: null Sph → null result ───────────────────────────────────────

    @Test
    @DisplayName("compute: should return null when Sph is null")
    void compute_shouldReturnNull_whenSphIsNull() {
        Double result = calculator.compute(null, -1.00);
        assertThat(result).isNull();
    }

    // ── Test 5: null Cyl → null result ───────────────────────────────────────

    @Test
    @DisplayName("compute: should return null when Cyl is null")
    void compute_shouldReturnNull_whenCylIsNull() {
        Double result = calculator.compute(-2.00, null);
        assertThat(result).isNull();
    }

    // ── Test 6: both null → null result ──────────────────────────────────────

    @Test
    @DisplayName("compute: should return null when both Sph and Cyl are null")
    void compute_shouldReturnNull_whenBothNull() {
        Double result = calculator.compute(null, null);
        assertThat(result).isNull();
    }

    // ── Test 7: rounding — midpoint rounds toward positive infinity ───────────

    @Test
    @DisplayName("compute: should round 0.125 up to 0.25 (nearest 0.25D step)")
    void compute_shouldRoundUp_atMidpoint() {
        // Sph=0.0, Cyl=0.25 → raw = 0.125; round(0.125 * 4) = round(0.5) = 1 → 1/4 = 0.25
        Double result = calculator.compute(0.0, 0.25);
        assertThat(result).isEqualTo(0.25);
    }

    // ── Test 8: large values ──────────────────────────────────────────────────

    @Test
    @DisplayName("compute: should handle high myopia values correctly")
    void compute_shouldHandleHighMyopia() {
        // Sph=-10.0, Cyl=-4.0 → raw = -12.0 → -12.0 (on step)
        Double result = calculator.compute(-10.0, -4.0);
        assertThat(result).isEqualTo(-12.0);
    }
}
