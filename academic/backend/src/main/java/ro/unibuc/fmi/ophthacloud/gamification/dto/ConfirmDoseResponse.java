package ro.unibuc.fmi.ophthacloud.gamification.dto;

import ro.unibuc.fmi.ophthacloud.gamification.domain.AdherenceStatus;

import java.time.Instant;

public record ConfirmDoseResponse(
        AdherenceStatus status,
        Instant confirmedAt,
        int confirmedToday,
        int expectedToday,
        int dailyProgressPercent,
        int currentStreak,
        int forgivenessShields,
        int totalPoints,
        boolean rewardsSwitchEnabled
) {
}
