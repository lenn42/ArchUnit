package com.tngtech.archunit.exampletest.junit5;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.example.AppModule;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;
import com.tngtech.archunit.library.modules.syntax.DescriptorFunction;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static java.util.stream.Collectors.toSet;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class ModulesTest {

    /**
     * This example demonstrates how to easily derive modules from classes annotated with a certain annotation.
     * Within the example those are simply package-info files which denote the root of the modules by
     * being annotated with @AppModule.
     */
    @ArchTest
    static ArchRule modules_should_respect_their_declared_dependencies__use_annotation_API =
            modules()
                    .definedByAnnotation(AppModule.class)
                    .should(respectTheirDeclaredDependenciesWithin("..example.."));

    /**
     * This example demonstrates how to use the slightly more generic root class API to define modules.
     * While the result in this example is the same as the above, this API in general can be used to
     * use arbitrary classes as roots of modules.
     * For example if there is always a central interface denoted in some way,
     * the modules could be derived from these interfaces.
     */
    @ArchTest
    static ArchRule modules_should_respect_their_declared_dependencies__use_root_class_API =
            modules()
                    .definedByRootClasses(
                            DescribedPredicate.describe("annotated with @" + AppModule.class.getSimpleName(), (JavaClass rootClass) ->
                                    rootClass.isAnnotatedWith(AppModule.class))
                    )
                    .derivingModuleFromRootClassBy(
                            DescribedFunction.describe("annotation @" + AppModule.class.getSimpleName(), (JavaClass rootClass) -> {
                                AppModule module = rootClass.getAnnotationOfType(AppModule.class);
                                return new AnnotationDescriptor<>(module.name(), module);
                            })
                    )
                    .should(respectTheirDeclaredDependenciesWithin("..example.."));

    /**
     * This example demonstrates how to use the generic API to define modules.
     * The result in this example again is the same as the above, however in general the generic API
     * allows to derive modules in a completely customizable way.
     */
    @ArchTest
    static ArchRule modules_should_respect_their_declared_dependencies__use_generic_API =
            modules()
                    .definedBy(identifierFromModulesAnnotation())
                    .derivingModule(fromModulesAnnotation())
                    .should(respectTheirDeclaredDependenciesWithin("..example.."));

    private static IdentifierFromAnnotation identifierFromModulesAnnotation() {
        return new IdentifierFromAnnotation();
    }

    private static DescriptorFunction<AnnotationDescriptor<AppModule>> fromModulesAnnotation() {
        return DescriptorFunction.describe(String.format("from @%s(name)", AppModule.class.getSimpleName()),
                (ArchModule.Identifier identifier, Set<JavaClass> containedClasses) -> {
                    JavaClass rootClass = containedClasses.stream().filter(it -> it.isAnnotatedWith(AppModule.class)).findFirst().get();
                    AppModule module = rootClass.getAnnotationOfType(AppModule.class);
                    return new AnnotationDescriptor<>(module.name(), module);
                });
    }

    private static DeclaredDependenciesCondition respectTheirDeclaredDependenciesWithin(String applicationRootPackageIdentifier) {
        return new DeclaredDependenciesCondition(applicationRootPackageIdentifier);
    }

    private static class DeclaredDependenciesCondition extends ArchCondition<ArchModule<AnnotationDescriptor<AppModule>>> {
        private final PackageMatcher applicationRootPackageMatcher;

        DeclaredDependenciesCondition(String applicationRootPackageIdentifier) {
            super("respect their declared dependencies within %s", applicationRootPackageIdentifier);
            this.applicationRootPackageMatcher = PackageMatcher.of(applicationRootPackageIdentifier);
        }

        @Override
        public void check(ArchModule<AnnotationDescriptor<AppModule>> module, ConditionEvents events) {
            Set<ModuleDependency<AnnotationDescriptor<AppModule>>> actualDependencies = module.getModuleDependenciesFromSelf();
            Set<String> allowedDependencyTargets = Arrays.stream(module.getDescriptor().getAnnotation().allowedDependencies()).collect(toSet());

            actualDependencies.stream()
                    .filter(it -> !allowedDependencyTargets.contains(it.getTarget().getName()))
                    .forEach(it -> events.add(violated(it, it.getDescription())));

            module.getUndefinedDependencies().stream()
                    .filter(it -> !it.getTargetClass().isEquivalentTo(AppModule.class))
                    .filter(it -> applicationRootPackageMatcher.matches(it.getTargetClass().getPackageName()))
                    .forEach(it -> events.add(violated(it, "Dependency not contained in any module: " + it.getDescription())));
        }
    }

    private static class IdentifierFromAnnotation extends DescribedFunction<JavaClass, ArchModule.Identifier> {
        IdentifierFromAnnotation() {
            super("root classes with annotation @" + AppModule.class.getSimpleName());
        }

        @Override
        public ArchModule.Identifier apply(JavaClass javaClass) {
            return getIdentifierOfPackage(javaClass.getPackage());
        }

        private ArchModule.Identifier getIdentifierOfPackage(JavaPackage javaPackage) {
            Optional<ArchModule.Identifier> identifierInCurrentPackage = javaPackage.getClasses().stream()
                    .filter(it -> it.isAnnotatedWith(AppModule.class))
                    .findFirst()
                    .map(annotatedClassInPackage -> ArchModule.Identifier.from(annotatedClassInPackage.getAnnotationOfType(AppModule.class).name()));

            return identifierInCurrentPackage.orElseGet(identifierInParentPackageOf(javaPackage));
        }

        private Supplier<ArchModule.Identifier> identifierInParentPackageOf(JavaPackage javaPackage) {
            return () -> javaPackage.getParent().isPresent()
                    ? getIdentifierOfPackage(javaPackage.getParent().get())
                    : ArchModule.Identifier.ignore();
        }
    }
}
