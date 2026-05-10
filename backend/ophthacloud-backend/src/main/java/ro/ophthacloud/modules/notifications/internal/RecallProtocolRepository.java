package ro.ophthacloud.modules.notifications.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecallProtocolRepository extends JpaRepository<RecallProtocolEntity, UUID> {
    List<RecallProtocolEntity> findByIcd10CodeAndIsActiveTrue(String icd10Code);
}
