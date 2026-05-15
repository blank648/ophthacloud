package ro.ophthacloud.modules.prescriptions;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.ophthacloud.modules.prescriptions.dto.CreatePrescriptionRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionDto;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionVerifyDto;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionService;
import ro.ophthacloud.modules.prescriptions.internal.PrescriptionStatusType;
import ro.ophthacloud.shared.api.PdfDownloadResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionsFacade {

    private final PrescriptionService prescriptionService;

    public PrescriptionDto createPrescription(UUID tenantId, UUID issuedById, String issuedByName,
                                              CreatePrescriptionRequest request) {
        return prescriptionService.createPrescription(tenantId, issuedById, issuedByName, request);
    }

    public PrescriptionDto getPrescription(UUID tenantId, UUID id) {
        return prescriptionService.getPrescription(tenantId, id);
    }

    public Page<PrescriptionDto> listPrescriptions(UUID tenantId, UUID patientId,
                                                    PrescriptionStatusType status, Pageable pageable) {
        return prescriptionService.listPrescriptions(tenantId, patientId, status, pageable);
    }

    public PrescriptionDto signPrescription(UUID tenantId, UUID id) {
        return prescriptionService.signPrescription(tenantId, id);
    }

    public PrescriptionDto cancelPrescription(UUID tenantId, UUID id) {
        return prescriptionService.cancelPrescription(tenantId, id);
    }

    public PrescriptionVerifyDto verifyByQrToken(UUID qrToken) {
        return prescriptionService.verifyByQrToken(qrToken);
    }

    public PdfDownloadResponse generatePdf(UUID tenantId, UUID id) {
        return prescriptionService.generatePdf(tenantId, id);
    }
}
