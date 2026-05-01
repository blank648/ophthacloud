package ro.ophthacloud.modules.prescriptions.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ophthacloud.modules.prescriptions.dto.CreatePrescriptionRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionDto;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionLineDto;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionLineRequest;
import ro.ophthacloud.modules.prescriptions.dto.PrescriptionVerifyDto;
import ro.ophthacloud.modules.prescriptions.event.PrescriptionSignedEvent;
import ro.ophthacloud.shared.tenant.TenantContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final String QR_BASE_URL = "https://app.ophthacloud.ro/verify/";

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionLineRepository prescriptionLineRepository;
    private final PrescriptionNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    // ── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public PrescriptionDto createPrescription(UUID tenantId, UUID issuedById, String issuedByName,
                                              CreatePrescriptionRequest request) {
        // Fetch patient MRN via JDBC to avoid cross-module entity dependency
        String mrn = jdbcTemplate.queryForObject(
                "SELECT mrn FROM patients WHERE id = ? AND tenant_id = ?",
                String.class, request.getPatientId(), tenantId);

        if (mrn == null) {
            throw new IllegalArgumentException("Patient not found: " + request.getPatientId());
        }

        String prescriptionNumber = numberGenerator.generate(mrn, tenantId, prescriptionRepository);

        // QR code token: UUID.randomUUID() only — no timestamp component per user spec
        UUID qrToken = UUID.randomUUID();

        PrescriptionEntity entity = PrescriptionEntity.builder()
                .patientId(request.getPatientId())
                .consultationId(request.getConsultationId())
                .prescriptionNumber(prescriptionNumber)
                .prescriptionType(request.getPrescriptionType())
                .status(PrescriptionStatusType.ACTIVE)
                .issuedById(issuedById)
                .issuedByName(issuedByName)
                .issuedAt(Instant.now())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .pdBinocular(request.getPdBinocular())
                .pdOd(request.getPdOd())
                .pdOs(request.getPdOs())
                .prismOd(request.getPrismOd())
                .prismOs(request.getPrismOs())
                .lensType(request.getLensType())
                .lensMaterial(request.getLensMaterial())
                .lensCoating(request.getLensCoating())
                .frameRecommendation(request.getFrameRecommendation())
                .clinicalNotes(request.getClinicalNotes())
                .patientInstructions(request.getPatientInstructions())
                .qrCodeToken(qrToken)
                .build();

        PrescriptionEntity saved = prescriptionRepository.save(entity);

        // Save prescription lines with auto-computed SEQ
        List<PrescriptionLineEntity> lines = request.getLines().stream()
                .map(lineReq -> buildLine(lineReq, saved.getId()))
                .collect(Collectors.toList());
        prescriptionLineRepository.saveAll(lines);

        log.info("createPrescription: id={}, number={}, patient={}", saved.getId(), prescriptionNumber, request.getPatientId());
        return mapToDto(saved, lines);
    }

    // ── Read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PrescriptionDto getPrescription(UUID tenantId, UUID id) {
        PrescriptionEntity entity = getEntityOrThrow(tenantId, id);
        List<PrescriptionLineEntity> lines = prescriptionLineRepository
                .findByTenantIdAndPrescriptionId(tenantId, id);
        return mapToDto(entity, lines);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> listPrescriptions(UUID tenantId, UUID patientId,
                                                    PrescriptionStatusType status, Pageable pageable) {
        Page<PrescriptionEntity> page;
        if (status != null) {
            page = prescriptionRepository.findByTenantIdAndPatientIdAndStatus(tenantId, patientId, status, pageable);
        } else {
            page = prescriptionRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable);
        }
        return page.map(entity -> {
            List<PrescriptionLineEntity> lines = prescriptionLineRepository
                    .findByTenantIdAndPrescriptionId(tenantId, entity.getId());
            return mapToDto(entity, lines);
        });
    }

    // ── Sign ────────────────────────────────────────────────────────────────

    /**
     * Doctor confirms/signs the prescription.
     * Supersedes any other ACTIVE prescription of the same type for this patient.
     * Publishes PrescriptionSignedEvent.
     * Per GUIDE_06 §5.3 and GUIDE_04 §6.4.
     */
    @Transactional
    public PrescriptionDto signPrescription(UUID tenantId, UUID id) {
        PrescriptionEntity entity = getEntityOrThrow(tenantId, id);

        if (entity.getStatus() == PrescriptionStatusType.CANCELLED) {
            throw new PrescriptionAlreadyCancelledException();
        }

        // Supersede previous active prescriptions of same type
        int superseded = prescriptionRepository.supersedePreviousActive(
                entity.getPatientId(), tenantId, entity.getPrescriptionType(), id);
        if (superseded > 0) {
            log.info("signPrescription: superseded {} previous {} prescriptions for patient={}",
                    superseded, entity.getPrescriptionType(), entity.getPatientId());
        }

        entity.setSignedAt(Instant.now());
        // Status stays ACTIVE — signing confirms the prescription
        PrescriptionEntity updated = prescriptionRepository.save(entity);

        eventPublisher.publishEvent(new PrescriptionSignedEvent(
                updated.getId(),
                updated.getPatientId(),
                updated.getTenantId(),
                updated.getPrescriptionType(),
                updated.getPrescriptionNumber(),
                updated.getSignedAt()
        ));

        List<PrescriptionLineEntity> lines = prescriptionLineRepository
                .findByTenantIdAndPrescriptionId(tenantId, id);
        return mapToDto(updated, lines);
    }

    // ── Cancel ──────────────────────────────────────────────────────────────

    @Transactional
    public PrescriptionDto cancelPrescription(UUID tenantId, UUID id) {
        PrescriptionEntity entity = getEntityOrThrow(tenantId, id);

        if (entity.getStatus() == PrescriptionStatusType.CANCELLED
                || entity.getStatus() == PrescriptionStatusType.EXPIRED) {
            throw new PrescriptionAlreadyCancelledException();
        }

        entity.setStatus(PrescriptionStatusType.CANCELLED);
        PrescriptionEntity updated = prescriptionRepository.save(entity);

        List<PrescriptionLineEntity> lines = prescriptionLineRepository
                .findByTenantIdAndPrescriptionId(tenantId, id);
        return mapToDto(updated, lines);
    }

    // ── Public QR Verify (no tenant context needed) ─────────────────────────

    @Transactional(readOnly = true)
    public PrescriptionVerifyDto verifyByQrToken(UUID qrToken) {
        PrescriptionEntity entity = prescriptionRepository.findByQrCodeToken(qrToken)
                .orElseThrow(() -> new PrescriptionNotFoundException(null));

        // Fetch patient name and DOB via JDBC to avoid cross-module dependency
        String[] patientData = jdbcTemplate.queryForObject(
                "SELECT first_name || ' ' || last_name, date_of_birth::text FROM patients WHERE id = ?",
                (rs, row) -> new String[]{rs.getString(1), rs.getString(2)},
                entity.getPatientId());

        String clinicName = jdbcTemplate.queryForObject(
                "SELECT name FROM tenants WHERE id = ?",
                String.class, entity.getTenantId());

        List<PrescriptionLineEntity> lines = prescriptionLineRepository
                .findByTenantIdAndPrescriptionId(entity.getTenantId(), entity.getId());

        return PrescriptionVerifyDto.builder()
                .prescriptionNumber(entity.getPrescriptionNumber())
                .patientName(patientData != null ? patientData[0] : null)
                .dateOfBirth(patientData != null ? patientData[1] : null)
                .prescriptionType(entity.getPrescriptionType().name())
                .status(entity.getStatus().name())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .issuedByName(entity.getIssuedByName())
                .clinicName(clinicName)
                .pdBinocular(entity.getPdBinocular())
                .lines(lines.stream().map(this::mapLineToDto).collect(Collectors.toList()))
                .build();
    }

    // ── Nightly Expiry Scheduler ─────────────────────────────────────────────

    /**
     * Runs daily at 01:00 to expire prescriptions past their valid_until date.
     * Per GUIDE_06 §5.4.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireOverduePrescriptions() {
        List<PrescriptionEntity> expired = prescriptionRepository.findExpired();
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(p -> p.setStatus(PrescriptionStatusType.EXPIRED));
        prescriptionRepository.saveAll(expired);
        log.info("expireOverduePrescriptions: expired {} prescriptions", expired.size());
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private PrescriptionEntity getEntityOrThrow(UUID tenantId, UUID id) {
        return prescriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
    }

    private PrescriptionLineEntity buildLine(PrescriptionLineRequest req, UUID prescriptionId) {
        BigDecimal seq = computeSeq(req.getSph(), req.getCyl());
        return PrescriptionLineEntity.builder()
                .prescriptionId(prescriptionId)
                .eye(req.getEye().toUpperCase())
                .sph(req.getSph())
                .cyl(req.getCyl())
                .axis(req.getAxis())
                .addPower(req.getAddPower())
                .vaSc(req.getVaSc())
                .vaCc(req.getVaCc())
                .bcva(req.getBcva())
                .seq(seq)
                .build();
    }

    /**
     * SEQ = Sph + Cyl/2, rounded to 0.25D. Per GUIDE_06 §3.2.
     * Uses Math.round semantics (rounds toward +infinity at 0.5 midpoints)
     * matching the existing SeqCalculator contract.
     * Example: Sph=-2.50, Cyl=-0.75 → raw=-2.875 → *4=-11.5 → Math.round→-11 → -2.75
     */
    private BigDecimal computeSeq(BigDecimal sph, BigDecimal cyl) {
        if (sph == null || cyl == null) {
            return null;
        }
        BigDecimal raw = sph.add(cyl.divide(BigDecimal.valueOf(2)));
        // Multiply by 4, round to nearest integer using Math.round (toward +infinity at 0.5),
        // then divide back by 4 and return with 2dp.
        long rounded = Math.round(raw.multiply(BigDecimal.valueOf(4)).doubleValue());
        return BigDecimal.valueOf(rounded).divide(BigDecimal.valueOf(4)).setScale(2, RoundingMode.HALF_UP);
    }

    private PrescriptionDto mapToDto(PrescriptionEntity entity, List<PrescriptionLineEntity> lines) {
        return PrescriptionDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .consultationId(entity.getConsultationId())
                .prescriptionNumber(entity.getPrescriptionNumber())
                .prescriptionType(entity.getPrescriptionType())
                .status(entity.getStatus())
                .issuedById(entity.getIssuedById())
                .issuedByName(entity.getIssuedByName())
                .issuedAt(entity.getIssuedAt())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .pdBinocular(entity.getPdBinocular())
                .pdOd(entity.getPdOd())
                .pdOs(entity.getPdOs())
                .prismOd(entity.getPrismOd())
                .prismOs(entity.getPrismOs())
                .lensType(entity.getLensType())
                .lensMaterial(entity.getLensMaterial())
                .lensCoating(entity.getLensCoating())
                .frameRecommendation(entity.getFrameRecommendation())
                .clinicalNotes(entity.getClinicalNotes())
                .patientInstructions(entity.getPatientInstructions())
                .supersededById(entity.getSupersededById())
                .qrCodeToken(entity.getQrCodeToken().toString())
                .qrVerifyUrl(QR_BASE_URL + entity.getQrCodeToken())
                .signedAt(entity.getSignedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lines(lines.stream().map(this::mapLineToDto).collect(Collectors.toList()))
                .build();
    }

    private PrescriptionLineDto mapLineToDto(PrescriptionLineEntity line) {
        return PrescriptionLineDto.builder()
                .eye(line.getEye())
                .sph(line.getSph())
                .cyl(line.getCyl())
                .axis(line.getAxis())
                .addPower(line.getAddPower())
                .vaSc(line.getVaSc())
                .vaCc(line.getVaCc())
                .bcva(line.getBcva())
                .seq(line.getSeq())
                .build();
    }
}
