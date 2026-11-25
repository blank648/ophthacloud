package ro.unibuc.fmi.ophthacloud.gamification.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayAdherenceResponse(
        LocalDate clinicDate,
        NextDoseDto nextDose,
        List<ScheduledDoseDto> doses,
        int confirmedToday,
        int expectedToday,
        int dailyProgressPercent,
        int currentStreak,
        int totalPoints,
        boolean rewardsSwitchEnabled
) {
    public TodayAdherenceResponse {
        doses = doses == null ? List.of() : List.copyOf(doses);
    }
}
