@org.springframework.modulith.ApplicationModule(
    displayName = "Portal",
    allowedDependencies = {
        "shared",
        "modules.patients",
        "modules.appointments",
        "modules.prescriptions",
        "modules.investigations",
        "modules.optical",
        "modules.notifications"
    }
)
package ro.ophthacloud.modules.portal;
