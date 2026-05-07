package ro.ophthacloud.modules.optical.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QcResultDto(
    @JsonProperty("valuesChecked")   boolean valuesChecked,
    @JsonProperty("opticalCenterOd") boolean opticalCenterOd,
    @JsonProperty("opticalCenterOs") boolean opticalCenterOs,
    @JsonProperty("pupillaryDistance") boolean pupillaryDistance,
    @JsonProperty("segmentHeight")   boolean segmentHeight,
    @JsonProperty("treatmentsApplied") boolean treatmentsApplied,
    @JsonProperty("assemblyQuality") boolean assemblyQuality,
    @JsonProperty("finalCleaning")   boolean finalCleaning
) {
    public boolean isAllPassed() {
        return valuesChecked && opticalCenterOd && opticalCenterOs &&
               pupillaryDistance && segmentHeight && treatmentsApplied &&
               assemblyQuality && finalCleaning;
    }
}
