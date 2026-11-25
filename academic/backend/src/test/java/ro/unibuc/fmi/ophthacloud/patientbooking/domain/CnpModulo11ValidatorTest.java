package ro.unibuc.fmi.ophthacloud.patientbooking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Official Team 19 JUnit 5 unit test template using Arrange-Act-Assert structure.
 */
class CnpModulo11ValidatorTest {

    private final CnpModulo11Validator validator = new CnpModulo11Validator();

    @Test
    void acceptsValidCnpWithRemainderBelowTen() {
        // Arrange
        String cnp = "1850515400018"; // weighted sum = 173, 173 % 11 = 8

        // Act
        boolean valid = validator.isValid(cnp);
        Optional<LocalDate> birthDate = validator.extractBirthDate(cnp);

        // Assert
        assertThat(valid).isTrue();
        assertThat(birthDate).contains(LocalDate.of(1985, 5, 15));
    }

    @Test
    void acceptsValidCnpWithRemainderZero() {
        // Arrange
        String cnp = "1850515400050"; // weighted sum = 209, 209 % 11 = 0

        // Act
        boolean valid = validator.isValid(cnp);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    void acceptsValidCnpWithRemainderTenMappedToOne() {
        // Arrange
        String cnp = "1850515400191"; // weighted sum = 252, 252 % 11 = 10 -> 1

        // Act
        boolean valid = validator.isValid(cnp);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    void rejectsInvalidChecksum() {
        // Arrange
        String cnpWithBadControlDigit = "1850515400019";

        // Act
        boolean valid = validator.isValid(cnpWithBadControlDigit);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    void rejectsWrongLengthTwelveOrFourteenDigits() {
        // Arrange
        String tooShort = "185051540001";
        String tooLong = "18505154000180";

        // Act
        boolean shortResult = validator.isValid(tooShort);
        boolean longResult = validator.isValid(tooLong);

        // Assert
        assertThat(shortResult).isFalse();
        assertThat(longResult).isFalse();
    }

    @Test
    void rejectsNonDigitCharacters() {
        // Arrange
        String alphaNumeric = "185051540001A";
        String withSpaces = "1850515 40001";

        // Act
        boolean alphaResult = validator.isValid(alphaNumeric);
        boolean spaceResult = validator.isValid(withSpaces);

        // Assert
        assertThat(alphaResult).isFalse();
        assertThat(spaceResult).isFalse();
    }

    @Test
    void rejectsNullAndBlankInputs() {
        // Arrange
        String nullInput = null;
        String blankInput = "             ";

        // Act
        boolean nullResult = validator.isValid(nullInput);
        boolean blankResult = validator.isValid(blankInput);

        // Assert
        assertThat(nullResult).isFalse();
        assertThat(blankResult).isFalse();
    }

    @Test
    void rejectsInvalidSexCenturyDigitEvenWhenChecksumMatches() {
        // Arrange
        String invalidSexDigitCnp = "9850515400012"; // weighted sum = 189, 189 % 11 = 2, but sex digit 9 is invalid

        // Act
        boolean valid = validator.isValid(invalidSexDigitCnp);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    void rejectsImpossibleCalendarDateEvenWhenChecksumMatches() {
        // Arrange
        String feb30Cnp = "1850230400014"; // 1985-02-30 with valid Modulo 11 checksum (sum = 158, 158 % 11 = 4)

        // Act
        boolean valid = validator.isValid(feb30Cnp);
        Optional<LocalDate> birthDate = validator.extractBirthDate(feb30Cnp);

        // Assert
        assertThat(valid).isFalse();
        assertThat(birthDate).isEmpty();
    }
}
