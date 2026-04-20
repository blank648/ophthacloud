package ro.ophthacloud.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA Auditing.
 * This ensures that @CreatedDate and @LastModifiedDate in {@link BaseEntity}
 * are automatically populated when entities are saved.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
