package ro.unibuc.fmi.ophthacloud.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "gamification_profiles")
public class GamificationProfileEntity {

    @Id
    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak;

    @Column(name = "forgiveness_shields", nullable = false)
    private int forgivenessShields;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unlocked_badges", nullable = false, columnDefinition = "jsonb")
    private List<String> unlockedBadges = new ArrayList<>();

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    protected GamificationProfileEntity() {
    }

    public GamificationProfileEntity(UUID patientId) {
        this.patientId = patientId;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.forgivenessShields = 0;
        this.totalPoints = 0;
        this.unlockedBadges = new ArrayList<>();
        this.lastCompletedDate = null;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public int getForgivenessShields() {
        return forgivenessShields;
    }

    public void setForgivenessShields(int forgivenessShields) {
        this.forgivenessShields = forgivenessShields;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public List<String> getUnlockedBadges() {
        return unlockedBadges;
    }

    public void setUnlockedBadges(List<String> unlockedBadges) {
        this.unlockedBadges = unlockedBadges != null ? new ArrayList<>(unlockedBadges) : new ArrayList<>();
    }

    public LocalDate getLastCompletedDate() {
        return lastCompletedDate;
    }

    public void setLastCompletedDate(LocalDate lastCompletedDate) {
        this.lastCompletedDate = lastCompletedDate;
    }
}
