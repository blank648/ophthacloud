package ro.ophthacloud.modules.patients.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import ro.ophthacloud.shared.enums.GenderType;

import java.time.LocalDate;

/**
 * Request DTO for fully replacing an existing patient record (PUT semantics).
 * Same required fields as {@link CreatePatientRequest} — the MRN is never updated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePatientRequest {

    @NotBlank(message = "Prenumele este obligatoriu.")
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank(message = "Numele de familie este obligatoriu.")
    @Size(min = 2, max = 100)
    private String lastName;

    @NotNull(message = "Data nașterii este obligatorie.")
    @Past(message = "Data nașterii nu poate fi în viitor.")
    private LocalDate dateOfBirth;

    @NotNull(message = "Genul este obligatoriu.")
    private GenderType gender;

    @Size(min = 10)
    private String phone;

    private String phoneAlt;

    @Email
    private String email;

    @Size(min = 13, max = 13)
    @Pattern(regexp = "\\d{13}")
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
