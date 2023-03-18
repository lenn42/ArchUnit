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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;
import com.tngtech.archunit.library.modules.ArchModule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface GivenModules<DESCRIPTOR extends ArchModule.Descriptor> extends GivenObjects<ArchModule<DESCRIPTOR>>, GivenConjunction<ArchModule<DESCRIPTOR>> {

    @Override
    GivenModules<DESCRIPTOR> that(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate);

    @Override
    GivenModules<DESCRIPTOR> and(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate);

    @Override
    GivenModules<DESCRIPTOR> or(DescribedPredicate<? super ArchModule<DESCRIPTOR>> predicate);

    /**
     * Allows to specify assertions for the set of {@link ArchModule}s under consideration. E.g.
     * <br><br>
     * <code>
     * {@link ModuleRuleDefinition#modules() modules()}.{@link GivenModules#should() should()}.{@link ModulesShould#respectTheirAllowedDependencies(DescribedPredicate, ModuleDependencyScope) respectTheirAllowedDependencies(..)}
     * </code>
     * <br>
     * Use {@link #should(ArchCondition)} to freely customize the condition against which the {@link ArchModule}s should be checked.
     *
     * @return A syntax element, which can be used to create rules for the {@link ArchModule}s under consideration
     */
    @PublicAPI(usage = ACCESS)
    ModulesShould<DESCRIPTOR> should();

    /**
     * Allows to adjust the description of the "given modules" part. E.g.
     * <pre><code>
     * modules().definedByAnnotation(AppModule.class).as("App Modules").should()...
     * </code></pre>
     * would yield a rule text "App Modules should...".
     */
    @PublicAPI(usage = ACCESS)
    GivenModules<DESCRIPTOR> as(String description, Object... args);
}
