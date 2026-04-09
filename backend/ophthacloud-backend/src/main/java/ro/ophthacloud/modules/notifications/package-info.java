@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "modules.patients",
        "modules.appointments",
        "modules.emr",
        "modules.prescriptions",
        "modules.optical"
    }
)
package ro.ophthacloud.modules.notifications;
