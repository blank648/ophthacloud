package ro.ophthacloud.shared.enums;

/**
 * Appointment status type matching the PostgreSQL enum {@code appointment_status_type}.
 * State machine: BOOKED → CONFIRMED → CHECKED_IN → IN_PROGRESS → COMPLETED (terminal)
 *                Any non-terminal → CANCELLED (terminal) or NO_SHOW (terminal)
 * Defined in GUIDE_03 §2 and state machine in GUIDE_06 §2.3.
 */
public enum AppointmentStatus {
    BOOKED,
    CONFIRMED,
    CHECKED_IN,
    IN_PROGRESS,
    COMPLETED,
    NO_SHOW,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }

    public boolean canTransitionTo(AppointmentStatus next) {
        return switch (this) {
            case BOOKED      -> next == CONFIRMED || next == CANCELLED || next == NO_SHOW;
            case CONFIRMED   -> next == CHECKED_IN || next == CANCELLED || next == NO_SHOW;
            case CHECKED_IN  -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == COMPLETED;
            default          -> false; // terminal states
        };
    }
}
