package ro.ophthacloud.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEquipmentRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String brand,
        @NotBlank @Size(max = 64) String type,
        @Size(max = 128) String location,
        @NotNull Boolean dicomEnabled
) {
}
