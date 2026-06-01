/**
 * Infrastructure cross-cutting concern — visible to all modules.
 * Contains base persistence models (BaseEntity, TenantAwareEntity) and common configs.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package ro.ophthacloud.infrastructure;
