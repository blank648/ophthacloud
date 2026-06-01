@org.springframework.modulith.ApplicationModule(
    displayName = "Portal",
    allowedDependencies = {
        "shared",
        "modules.patients",
        "modules.patients::dto",
        "modules.appointments",
        "modules.appointments::dto",
        "modules.prescriptions",
        "modules.prescriptions::dto",
        "modules.investigations",
        "modules.investigations::dto",
        "modules.optical",
        "modules.optical::dto",
        "modules.notifications",
        "modules.notifications::dto"
    }
)
package ro.ophthacloud.modules.portal;
