@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "modules.patients",
        "modules.appointments",
        "modules.prescriptions",
        "modules.optical"
    }
)
package ro.ophthacloud.modules.portal;
