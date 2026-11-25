package ro.unibuc.fmi.ophthacloud.gamification.dto;

import ro.unibuc.fmi.ophthacloud.common.api.PagedResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdherenceSummaryResponse(
        LocalDate from,
        LocalDate to,
        int expected,
        int confirmedOnTime,
        int confirmedLate,
        int missed,
        int pending,
        BigDecimal adherencePercent,
        PagedResponse<AdherenceHistoryItemDto> history
) {
}
