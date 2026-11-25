package ro.unibuc.fmi.ophthacloud.clinicalamsler.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TreatmentPlanQueryService {

    List<TreatmentPlanSnapshotDto> findPlansForPatientInWindow(UUID patientId, LocalDate from, LocalDate to);
}
