/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.modules.syntax;

import java.lang.annotation.Annotation;
import java.util.function.Function;
import java.util.function.Predicate;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ArchModules;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * @see #modules()
 */
@PublicAPI(usage = ACCESS)
public final class ModuleRuleDefinition {
    private ModuleRuleDefinition() {
    }

    /**
     * Entrypoint to define {@link ArchRule rules} based on {@link ArchModules}.
     *
     * @return A syntax element to create {@link ArchModules} {@link ArchRule rules}
     */
    @PublicAPI(usage = ACCESS)
    public static Creator modules() {
        return new Creator();
    }

    @PublicAPI(usage = ACCESS)
    public static final class Creator {
        private Creator() {
        }

        /**
         * @see ArchModules#defineBy(ArchModules.IdentifierAssociation)
         */
        @PublicAPI(usage = ACCESS)
        public GenericDefinition definedBy(DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            return new GenericDefinition(identifierFunction);
        }

        /**
         * @see ArchModules#defineByRootClasses(Predicate)
         */
        @PublicAPI(usage = ACCESS)
        public RootClassesDefinition definedByRootClasses(DescribedPredicate<? super JavaClass> predicate) {
            return new RootClassesDefinition(predicate);
        }

        /**
         * @see ArchModules#defineByAnnotation(Class)
         */
        @PublicAPI(usage = ACCESS)
        public <A extends Annotation> GivenModules<AnnotationDescriptor<A>> definedByAnnotation(Class<A> annotationType) {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineByAnnotation(annotationType)
                    .modularize(classes)
            ).as("modules defined by annotation @%s", annotationType.getSimpleName());
        }

        /**
         * @see ArchModules#defineByAnnotation(Class, Function)
         */
        @PublicAPI(usage = ACCESS)
        public <A extends Annotation> GivenModules<AnnotationDescriptor<A>> definedByAnnotation(Class<A> annotationType, Function<A, String> nameFunction) {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineByAnnotation(annotationType, nameFunction)
                    .modularize(classes)
            ).as("modules defined by annotation @%s", annotationType.getSimpleName());
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final class RootClassesDefinition {
        private final DescribedPredicate<? super JavaClass> rootClassPredicate;
        private final String descriptionStart;

        private RootClassesDefinition(DescribedPredicate<? super JavaClass> rootClassPredicate) {
            this.rootClassPredicate = rootClassPredicate;
            descriptionStart = "modules defined by root classes " + rootClassPredicate.getDescription();
        }

        /**
         * @see ArchModules.CreatorByRootClass#describeModuleByRootClass(ArchModules.RootClassDescriptorCreator)
         */
        @PublicAPI(usage = ACCESS)
        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModuleFromRootClassBy(DescribedFunction<JavaClass, D> descriptorFunction) {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineByRootClasses(rootClassPredicate)
                    .describeModuleByRootClass((__, rootClass) -> descriptorFunction.apply(rootClass))
                    .modularize(classes)
            ).as(descriptionStart + " deriving module from root class by " + descriptorFunction.getDescription());
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final class GenericDefinition {
        private final DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction;

        private GenericDefinition(DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            this.identifierFunction = identifierFunction;
        }

        /**
         * @see ArchModules.Creator#describeBy(ArchModules.DescriptorCreator)
         */
        @PublicAPI(usage = ACCESS)
        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModule(DescriptorFunction<D> descriptorFunction) {
            return new GivenModulesInternal<>(classes -> ArchModules
                    .defineBy(identifierFunction::apply)
                    .describeBy(descriptorFunction::apply)
                    .modularize(classes)
            ).as("modules defined by %s deriving module %s", identifierFunction.getDescription(), descriptorFunction.getDescription());
        }
    }
}
