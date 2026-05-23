package ro.ophthacloud.modules.portal.dto;

import java.math.BigDecimal;

/**
 * Single prescription line for portal display.
 */
public record PortalPrescriptionLineDto(
        String eye,
        String lensType,
        BigDecimal sphere,
        BigDecimal cylinder,
        Integer axis,
        BigDecimal add,
        BigDecimal pd
) {}
