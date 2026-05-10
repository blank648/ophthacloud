@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "infrastructure",
        "modules.patients",
        "modules.patients::dto",
        "modules.appointments",
        "modules.appointments::event",
        "modules.emr",
        "modules.emr::event",
        "modules.prescriptions",
        "modules.prescriptions::event",
        "modules.optical",
        "modules.optical::event"
    }
)
package ro.ophthacloud.modules.notifications;
