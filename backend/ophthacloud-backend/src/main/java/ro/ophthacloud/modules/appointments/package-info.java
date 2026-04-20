@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "modules.patients", "modules.patients::dto", "infrastructure"}
)
package ro.ophthacloud.modules.appointments;
