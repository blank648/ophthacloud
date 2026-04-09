/**
 * Shared cross-cutting infrastructure — visible to all modules.
 * Contains security, API response wrappers, audit logging, tenant context, and utilities.
 * This is NOT a business module — it has no Facade, no Controller, no JPA entities of its own.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package ro.ophthacloud.shared;
