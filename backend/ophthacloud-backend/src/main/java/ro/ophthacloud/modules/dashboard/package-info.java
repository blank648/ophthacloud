@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "modules.patients",
        "modules.appointments",
        "modules.emr",
        "modules.investigations",
        "modules.prescriptions",
        "modules.optical",
        "modules.notifications",
        "modules.reports"
    }
)
package ro.ophthacloud.modules.dashboard;
