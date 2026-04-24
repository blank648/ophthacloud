package ro.ophthacloud.modules.emr.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationSectionRepository extends JpaRepository<ConsultationSectionEntity, UUID> {
    Optional<ConsultationSectionEntity> findByConsultationIdAndSectionCode(UUID consultationId, String sectionCode);
    List<ConsultationSectionEntity> findAllByConsultationId(UUID consultationId);
}
