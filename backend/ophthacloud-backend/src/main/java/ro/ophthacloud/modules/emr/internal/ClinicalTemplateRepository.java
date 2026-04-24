package ro.ophthacloud.modules.emr.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicalTemplateRepository extends JpaRepository<ClinicalTemplateEntity, UUID> {
    List<ClinicalTemplateEntity> findBySectionCode(String sectionCode);
}
