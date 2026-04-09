package ro.ophthacloud.shared.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Utility methods for date/time conversions used across all modules.
 * <p>
 * The API contract (GUIDE_04 §1.5) mandates:
 * <ul>
 *   <li>All API timestamps are UTC ISO-8601: {@code "2026-04-02T09:30:00Z"}</li>
 *   <li>All dates are {@code "YYYY-MM-DD"}</li>
 *   <li>Frontend displays in {@code Europe/Bucharest} — backend never converts for display</li>
 * </ul>
 */
public final class DateTimeUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTimeUtils() {
        // utility class
    }

    /** Converts a {@link LocalDate} to a midnight {@link Instant} in UTC. */
    public static Instant toUtcInstant(LocalDate date) {
        return date.atStartOfDay(UTC).toInstant();
    }

    /** Converts a {@link LocalDateTime} assumed to be in UTC to an {@link Instant}. */
    public static Instant toUtcInstant(LocalDateTime utcDateTime) {
        return utcDateTime.toInstant(ZoneOffset.UTC);
    }

    /** Converts an {@link Instant} to a {@link LocalDate} in UTC. */
    public static LocalDate toUtcDate(Instant instant) {
        return instant.atZone(UTC).toLocalDate();
    }

    /** Converts an {@link Instant} to a {@link LocalDate} in European/Bucharest timezone.
     *  Use this only for display-logic such as "today appointments" KPI queries. */
    public static LocalDate toBucharestDate(Instant instant) {
        return instant.atZone(BUCHAREST).toLocalDate();
    }

    /** Returns today's date in UTC. */
    public static LocalDate todayUtc() {
        return LocalDate.now(UTC);
    }

    /** Formats a {@link LocalDate} to ISO-8601 string {@code "YYYY-MM-DD"}. */
    public static String formatDate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    /**
     * Computes age in full years from date of birth to today (UTC).
     * Used by patient DTOs — age is never stored, always computed at runtime.
     */
    public static int ageInYears(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, todayUtc()).getYears();
    }
}
