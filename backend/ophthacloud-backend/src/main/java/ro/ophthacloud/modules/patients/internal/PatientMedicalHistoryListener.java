package ro.ophthacloud.modules.patients.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ro.ophthacloud.modules.emr.event.ConsultationSignedEvent;
import ro.ophthacloud.modules.patients.dto.ActiveDiagnosis;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientMedicalHistoryListener {

    private final PatientMedicalHistoryRepository historyRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @ApplicationModuleListener
    public void onConsultationSigned(ConsultationSignedEvent event) {
        log.debug("onConsultationSigned: consultationId={}, patientId={}", event.consultationId(), event.patientId());
        try {
            // Load Section F (Diagnostice) data from consultation_sections
            String sql = "SELECT section_data FROM consultation_sections WHERE consultation_id = ? AND section_code = 'F'";
            String sectionDataJson = jdbcTemplate.queryForObject(sql, String.class, event.consultationId());
            
            if (sectionDataJson == null || sectionDataJson.isBlank()) {
                return;
            }

            JsonNode root = objectMapper.readTree(sectionDataJson);
            JsonNode diagnosesNode = root.path("diagnoses");
            if (diagnosesNode.isMissingNode() || !diagnosesNode.isArray()) {
                return;
            }

            List<ActiveDiagnosis> newDiagnoses = new ArrayList<>();
            LocalDate sinceDate = LocalDate.now();

            for (JsonNode node : diagnosesNode) {
                String code = node.path("code").asString();
                String name = node.path("name").asString();
                if (code != null && !code.isBlank()) {
                    newDiagnoses.add(new ActiveDiagnosis(code, name, "OU", sinceDate));
                }
            }

            if (newDiagnoses.isEmpty()) {
                return;
            }

            // Find or create medical history for patient
            Optional<PatientMedicalHistoryEntity> historyOpt = historyRepository.findByPatientId(event.patientId());
            PatientMedicalHistoryEntity history;
            if (historyOpt.isPresent()) {
                history = historyOpt.get();
            } else {
                log.warn("patient_medical_history record not found for patient {}", event.patientId());
                return;
            }

            List<ActiveDiagnosis> currentDiagnoses = history.getActiveDiagnoses();
            if (currentDiagnoses == null) {
                currentDiagnoses = new ArrayList<>();
            } else {
                currentDiagnoses = new ArrayList<>(currentDiagnoses); // make mutable
            }

            // Merge avoiding duplicates based on icd10Code
            for (ActiveDiagnosis newDiag : newDiagnoses) {
                if (currentDiagnoses.stream().noneMatch(d -> d.icd10Code().equalsIgnoreCase(newDiag.icd10Code()))) {
                    currentDiagnoses.add(newDiag);
                }
            }

            history.setActiveDiagnoses(currentDiagnoses);
            historyRepository.save(history);
            log.info("Successfully updated active diagnoses for patient: {} from EMR consultation", event.patientId());

        } catch (Exception e) {
            log.error("Failed to update active diagnoses for patient {} from consultation {}: {}", 
                    event.patientId(), event.consultationId(), e.getMessage(), e);
        }
    }
}
