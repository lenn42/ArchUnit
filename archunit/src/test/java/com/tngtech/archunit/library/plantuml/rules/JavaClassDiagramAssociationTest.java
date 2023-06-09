package com.tngtech.archunit.library.plantuml.rules;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.diagramtests.confusingpackagenames.foopackage.barpackage.ClassInFooAndBarPackage;
import com.tngtech.archunit.library.diagramtests.simpledependency.origin.SomeOriginClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaClassDiagramAssociationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void get_package_identifier_associated_with_class() {
        String expectedPackageIdentifier = SomeOriginClass.class.getPackage().getName().replaceAll(".*\\.", "..");
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes(expectedPackageIdentifier)
                .component("B").withStereoTypes("..noclasshere")
                .write());

        JavaClass clazz = importClassWithContext(SomeOriginClass.class);

        assertThat(javaClassDiagramAssociation.getPackageIdentifiersFromComponentOf(clazz))
                .as("package identifiers of " + clazz.getName())
                .containsOnly(expectedPackageIdentifier);
    }

    @Test
    public void get_target_package_identifiers_of_class() {
        String expectedTarget1 = "..target1";
        String expectedTarget2 = "..target2";
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes(SomeOriginClass.class.getPackage().getName().replaceAll(".*\\.", ".."))
                .component("B").withStereoTypes(expectedTarget1)
                .component("C").withStereoTypes(expectedTarget2)
                .dependencyFrom("[A]").to("[B]")
                .dependencyFrom("[A]").to("[C]")
                .write());

        JavaClass clazz = importClassWithContext(SomeOriginClass.class);

        assertThat(javaClassDiagramAssociation.getTargetPackageIdentifiers(clazz))
                .as("package identifiers of " + clazz.getName())
                .containsOnly(expectedTarget1, expectedTarget2);
    }

    @Test
    public void rejects_class_not_contained_in_any_component() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("SomeComponent").withStereoTypes("..someStereotype.")
                .write());
        JavaClass classNotContained = importClassWithContext(Object.class);

        assertThatThrownBy(
                () -> javaClassDiagramAssociation.getTargetPackageIdentifiers(classNotContained)
        )
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void reports_if_class_is_contained_in_any_component() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("Object").withStereoTypes(Object.class.getPackage().getName())
                .write());

        assertThat(javaClassDiagramAssociation.contains(importClassWithContext(Object.class)))
                .as("association contains " + Object.class.getName()).isTrue();
        assertThat(javaClassDiagramAssociation.contains(importClassWithContext(File.class)))
                .as("association contains " + File.class.getName()).isFalse();
    }

    @Test
    public void rejects_class_residing_in_multiple_packages() {
        JavaClassDiagramAssociation javaClassDiagramAssociation = createAssociation(TestDiagram.in(temporaryFolder)
                .component("A").withStereoTypes("..foopackage..")
                .component("B").withStereoTypes("..barpackage")
                .write());
        JavaClass classContainedInTwoComponents = importClassWithContext(ClassInFooAndBarPackage.class);

        assertThatThrownBy(
                () -> javaClassDiagramAssociation.getTargetPackageIdentifiers(classContainedInTwoComponents)
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void rejects_duplicate_stereotype() {
        File file = TestDiagram.in(temporaryFolder)
                .component("first").withStereoTypes("..identical..")
                .component("second").withStereoTypes("..identical..")
                .write();

        assertThatThrownBy(() -> createAssociation(file))
                .isInstanceOf(IllegalDiagramException.class)
                .hasMessage("Stereotype '..identical..' should be unique");
    }

    private JavaClassDiagramAssociation createAssociation(File file) {
        PlantUmlDiagram diagram = new PlantUmlParser().parse(toUrl(file));
        return new JavaClassDiagramAssociation(diagram);
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
