package ro.ophthacloud.modules.patients.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.LocalDate;

/**
 * Request DTO for creating a new patient.
 * Required fields per GUIDE_04 §2.2: firstName, lastName, dateOfBirth, gender.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePatientRequest {

    @NotBlank(message = "Prenumele este obligatoriu.")
    @Size(min = 2, max = 100, message = "Prenumele trebuie să aibă între 2 și 100 de caractere.")
    private String firstName;

    @NotBlank(message = "Numele de familie este obligatoriu.")
    @Size(min = 2, max = 100, message = "Numele de familie trebuie să aibă între 2 și 100 de caractere.")
    private String lastName;

    @NotNull(message = "Data nașterii este obligatorie.")
    @Past(message = "Data nașterii nu poate fi în viitor.")
    private LocalDate dateOfBirth;

    @NotNull(message = "Genul este obligatoriu.")
    private GenderType gender;

    @Size(min = 10, message = "Numărul de telefon trebuie să aibă minim 10 caractere.")
    private String phone;

    private String phoneAlt;

    @Email(message = "Adresa de email nu este validă.")
    private String email;

    @Size(min = 13, max = 13, message = "CNP-ul trebuie să aibă exact 13 caractere.")
    @Pattern(regexp = "\\d{13}", message = "CNP-ul trebuie să conțină doar cifre.")
    private String cnp;

    private String address;
    private String city;
    private String county;
    private String bloodType;
    private String occupation;
    private String employer;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String insuranceProvider;
    private String insuranceNumber;
    private String referringDoctor;
    private String notes;
}
