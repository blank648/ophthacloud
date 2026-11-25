package ro.unibuc.fmi.ophthacloud;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "ro.unibuc.fmi.ophthacloud",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ModuleArchitectureTest {

    private static final String BASE_PACKAGE = "ro.unibuc.fmi.ophthacloud";
    private static final List<String> DOMAIN_MODULES = List.of(
            "patientbooking",
            "clinicalamsler",
            "gamification",
            "securityaudit"
    );

    @ArchTest
    static final ArchRule patientbooking_must_not_access_other_module_internals =
            noModuleInternalDependencies("patientbooking");

    @ArchTest
    static final ArchRule clinicalamsler_must_not_access_other_module_internals =
            noModuleInternalDependencies("clinicalamsler");

    @ArchTest
    static final ArchRule gamification_must_not_access_other_module_internals =
            noModuleInternalDependencies("gamification");

    @ArchTest
    static final ArchRule securityaudit_must_not_access_other_module_internals =
            noModuleInternalDependencies("securityaudit");

    @ArchTest
    static final ArchRule common_must_not_depend_on_domain_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..patientbooking..",
                            "..clinicalamsler..",
                            "..gamification..",
                            "..securityaudit.."
                    );

    @ArchTest
    static final ArchRule domain_modules_must_be_free_of_cycles =
            slices()
                    .matching(BASE_PACKAGE + ".(*)..")
                    .should().beFreeOfCycles();

    private static ArchRule noModuleInternalDependencies(String currentModule) {
        return noClasses()
                .that().resideInAPackage(".." + currentModule + "..")
                .should().dependOnClassesThat(inOtherModuleInternals(currentModule));
    }

    private static DescribedPredicate<JavaClass> inOtherModuleInternals(String currentModule) {
        return new DescribedPredicate<>("classes in non-api internal packages of other domain modules") {
            @Override
            public boolean test(JavaClass javaClass) {
                String pkg = javaClass.getPackageName();
                for (String module : DOMAIN_MODULES) {
                    if (module.equals(currentModule)) {
                        continue;
                    }
                    String modulePrefix = BASE_PACKAGE + "." + module;
                    String apiPrefix = modulePrefix + ".api";
                    boolean inModule = pkg.equals(modulePrefix) || pkg.startsWith(modulePrefix + ".");
                    boolean inApi = pkg.equals(apiPrefix) || pkg.startsWith(apiPrefix + ".");
                    if (inModule && !inApi) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
