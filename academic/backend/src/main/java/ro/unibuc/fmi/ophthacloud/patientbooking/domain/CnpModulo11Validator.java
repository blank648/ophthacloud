package ro.unibuc.fmi.ophthacloud.patientbooking.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Validates Romanian CNP (Cod Numeric Personal) strings using format, sex/century,
 * calendar date, and Modulo 11 control digit rules.
 */
public final class CnpModulo11Validator {

    private static final int CNP_LENGTH = 13;
    private static final int[] CONTROL_WEIGHTS = {2, 7, 9, 1, 4, 6, 3, 5, 8, 2, 7, 9};

    public boolean isValid(String cnp) {
        return extractBirthDate(cnp).isPresent();
    }

    public Optional<LocalDate> extractBirthDate(String cnp) {
        if (cnp == null || cnp.length() != CNP_LENGTH) {
            return Optional.empty();
        }

        for (int i = 0; i < CNP_LENGTH; i++) {
            char ch = cnp.charAt(i);
            if (ch < '0' || ch > '9') {
                return Optional.empty();
            }
        }

        int sexCenturyDigit = cnp.charAt(0) - '0';
        int centuryBase = resolveCenturyBase(sexCenturyDigit);
        if (centuryBase < 0) {
            return Optional.empty();
        }

        int yy = parseTwoDigits(cnp, 1);
        int month = parseTwoDigits(cnp, 3);
        int day = parseTwoDigits(cnp, 5);

        LocalDate birthDate;
        try {
            birthDate = LocalDate.of(centuryBase + yy, month, day);
        } catch (DateTimeException ex) {
            return Optional.empty();
        }

        int sum = 0;
        for (int i = 0; i < CONTROL_WEIGHTS.length; i++) {
            int digit = cnp.charAt(i) - '0';
            sum += digit * CONTROL_WEIGHTS[i];
        }
        int remainder = sum % 11;
        int expectedControlDigit = remainder == 10 ? 1 : remainder;
        int actualControlDigit = cnp.charAt(CNP_LENGTH - 1) - '0';

        if (actualControlDigit != expectedControlDigit) {
            return Optional.empty();
        }

        return Optional.of(birthDate);
    }

    private static int resolveCenturyBase(int sexCenturyDigit) {
        return switch (sexCenturyDigit) {
            case 1, 2, 7, 8 -> 1900;
            case 3, 4 -> 1800;
            case 5, 6 -> 2000;
            default -> -1;
        };
    }

    private static int parseTwoDigits(String value, int offset) {
        return (value.charAt(offset) - '0') * 10 + (value.charAt(offset + 1) - '0');
    }
}
