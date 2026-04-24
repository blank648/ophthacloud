package ro.ophthacloud.modules.emr.internal;

import org.springframework.stereotype.Component;

/**
 * Computes the Spherical Equivalent (SEQ) following the standard clinical formula.
 *
 * <p>Formula: {@code SEQ = Sph + Cyl / 2}
 * <p>Result is rounded to the nearest 0.25D (standard clinical step):
 * {@code Math.round(seq * 4.0) / 4.0}
 *
 * <p>Per GUIDE_06 §3.2.
 */
@Component
public class SeqCalculator {

    /**
     * Computes the SEQ for one eye.
     *
     * @param sph spherical power (dioptries); null → returns null
     * @param cyl cylindrical power (dioptries); null → returns null
     * @return SEQ rounded to nearest 0.25D, or null if either input is null
     */
    public Double compute(Double sph, Double cyl) {
        if (sph == null || cyl == null) {
            return null;
        }
        double raw = sph + (cyl / 2.0);
        // Round to nearest 0.25D
        return Math.round(raw * 4.0) / 4.0;
    }
}
