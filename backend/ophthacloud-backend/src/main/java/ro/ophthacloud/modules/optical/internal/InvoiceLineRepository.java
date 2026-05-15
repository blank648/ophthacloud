package ro.ophthacloud.modules.optical.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, UUID> {

    List<InvoiceLineEntity> findByInvoiceId(UUID invoiceId);
}
