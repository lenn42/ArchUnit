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
package com.tngtech.archunit.library.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingCollection;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.library.dependencies.Slices;
import com.tngtech.archunit.library.modules.ArchModule.Identifier;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Multimaps.asMap;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.core.domain.PackageMatcher.TO_GROUPS;
import static com.tngtech.archunit.library.modules.ArchModule.Identifier.ignore;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * A collection of {@link ArchModule "architectural modules"}. This class provides a convenient API to partition the {@link JavaClass classes}
 * of a code base into (cohesive) modules and assert properties of these modules. E.g. their dependencies to each other or dependencies
 * not contained in any of the constructed modules.<br>
 * This class provides several entry points to create {@link ArchModule modules} from a set of {@link JavaClass classes} in a convenient way:<br>
 * <ul>
 *     <li>{@link #defineBy(Function)} - the most generic API allowing a lot of flexibility</li>
 *     <li>{@link #defineByPackages(String)} - an API similar to {@link Slices#matching(String)}</li>
 * </ul>
 */
@PublicAPI(usage = ACCESS)
public final class ArchModules extends ForwardingCollection<ArchModule> {
    private final Map<Identifier, ArchModule> modulesByIdentifier;
    private final Map<String, ArchModule> modulesByName;

    private ArchModules(Set<ArchModule> modules) {
        this.modulesByIdentifier = modules.stream().collect(toMap(ArchModule::getIdentifier, identity()));
        this.modulesByName = modules.stream().collect(toMap(ArchModule::getName, identity()));

        SetMultimap<ArchModule.Identifier, ModuleDependency> moduleDependenciesByOrigin = HashMultimap.create();
        modules.forEach(it -> moduleDependenciesByOrigin.putAll(it.getIdentifier(), createModuleDependencies(it, modules)));

        SetMultimap<ArchModule.Identifier, ModuleDependency> moduleDependenciesByTarget = HashMultimap.create();
        moduleDependenciesByOrigin.values().forEach(it -> moduleDependenciesByTarget.put(it.getTarget().getIdentifier(), it));

        modules.forEach(it -> it.setModuleDependencies(moduleDependenciesByOrigin.get(it.getIdentifier()), moduleDependenciesByTarget.get(it.getIdentifier())));
    }

    private ImmutableSet<ModuleDependency> createModuleDependencies(ArchModule origin, Set<ArchModule> modules) {
        ImmutableSet.Builder<ModuleDependency> moduleDependencies = ImmutableSet.builder();
        for (ArchModule target : Sets.difference(modules, origin)) {
            ModuleDependency.tryCreate(origin, target).ifPresent(moduleDependencies::add);
        }
        return moduleDependencies.build();
    }

    @Override
    protected Collection<ArchModule> delegate() {
        return modulesByIdentifier.values();
    }

    /**
     * @param identifier The (textual) parts of an {@link ArchModule.Identifier}.
     * @return The contained {@link ArchModule} having an {@link ArchModule.Identifier} comprised of the passed {@code identifier} parts.
     *         This method will throw an exception if no matching {@link ArchModule} is contained.
     */
    @PublicAPI(usage = ACCESS)
    public ArchModule getByIdentifier(String... identifier) {
        return tryGetByIdentifier(identifier).orElseThrow(() ->
                new IllegalArgumentException(String.format("There is no %s with identifier %s", ArchModule.class.getSimpleName(), Arrays.toString(identifier))));
    }

    /**
     * @param identifier The (textual) parts of an {@link ArchModule.Identifier}.
     * @return The contained {@link ArchModule} having an {@link ArchModule.Identifier} comprised of the passed {@code identifier} parts.
     *         This method will throw an exception if no matching {@link ArchModule} is contained.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<ArchModule> tryGetByIdentifier(String... identifier) {
        return Optional.ofNullable(modulesByIdentifier.get(Identifier.from(identifier)));
    }

    /**
     * @param name The name of an {@link ArchModule}
     * @return A contained {@link ArchModule} with the passed {@link ArchModule#getName() name}.
     *         This method will throw an exception if no matching {@link ArchModule} is contained.
     * @see #tryGetByName(String)
     */
    @PublicAPI(usage = ACCESS)
    public ArchModule getByName(String name) {
        return tryGetByName(name).orElseThrow(() ->
                new IllegalArgumentException(String.format("There is no %s with name %s", ArchModule.class.getSimpleName(), name)));
    }

    /**
     * @param name The name of an {@link ArchModule}
     * @return A contained {@link ArchModule} with the passed {@link ArchModule#getName() name} or {@link Optional#empty()}
     *         if no matching {@link ArchModule} is contained.
     * @see #getByName(String)
     */
    @PublicAPI(usage = ACCESS)
    public Optional<ArchModule> tryGetByName(String name) {
        return Optional.ofNullable(modulesByName.get(name));
    }

    /**
     * @return The names of all {@link ArchModule modules} contained within these {@link ArchModules}
     */
    @PublicAPI(usage = ACCESS)
    public Set<String> getNames() {
        return modulesByName.keySet().stream().map(toStringFunction()).collect(toImmutableSet());
    }

    /**
     * Entrypoint to create {@link ArchModules} by partitioning a set of {@link JavaClass classes} into specific packages
     * matching the supplied {@link PackageMatcher packageIdentifier}.<br>
     *
     * Partitioning is done according to capturing groups. For example
     * <p>
     * Suppose there are three classes:<br><br>
     * {@code com.example.module.one.SomeClass}<br>
     * {@code com.example.module.one.AnotherClass}<br>
     * {@code com.example.module.two.YetAnotherClass}<br><br>
     * If modules are created by specifying<br><br>
     * {@code ArchModules.defineByPackages("..module.(*)..").modularize(javaClasses)}<br><br>
     * then the result will be two {@link ArchModule modules}, the {@link ArchModule module} where the capturing group is 'one'
     * and the {@link ArchModule module} where the capturing group is 'two'. The first {@link ArchModule module} will have
     * an {@link ArchModule.Identifier} consisting just of the single string {@code "one"}, while the latter will have
     * an {@link ArchModule.Identifier} consisting of the single string {@code "two"}.
     * If multiple packages would be matched, e.g. by {@code "..module.(*).(*).."}, the respective {@link ArchModule.Identifier}
     * would contain the two matched (sub-)package names as its {@link ArchModule.Identifier#getPart(int) parts}.
     * </p>
     *
     * @param packageIdentifier A {@link PackageMatcher package identifier}
     * @return A fluent API to further customize how to create {@link ArchModules}
     */
    @PublicAPI(usage = ACCESS)
    public static Creator defineByPackages(String packageIdentifier) {
        return defineBy(identifierByPackage(packageIdentifier));
    }

    private static IdentifierAssociation identifierByPackage(final String packageIdentifier) {
        return javaClass -> {
            PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);
            Optional<PackageMatcher.Result> result = packageMatcher.match(javaClass.getPackageName());
            return result.map(TO_GROUPS).map(Identifier::from).orElse(ignore());
        };
    }

    /**
     * Entrypoint to create {@link ArchModules} by partitioning a set of {@link JavaClass classes} into packages
     * defined by specific "root classes". The {@code rootClassPredicate} will determine which {@link JavaClass classes}
     * are root classes. {@link ArchModules} are formed by grouping together all classes that reside in the same package
     * or a subpackage of the respective root class. Thus, the packages of the defined root classes may not overlap,
     * i.e. no root class must reside in the same or a subpackage of another root class. All {@link JavaClass classes}
     * not contained in any package induced by a root class will be ignored from the derived {@link ArchModules}.<br>
     *
     * <p>
     * Take for example the following three classes:<br><br>
     * {@code com.example.module.one.SomeClass}<br>
     * {@code com.example.module.one.AnotherClass}<br>
     * {@code com.example.module.two.SomeOtherClass}<br><br>
     * Then the {@code rootClassPredicate}
     * <pre><code>
     * javaClass -> javaClass.getSimpleName().startsWith("Some")
     * </code></pre>
     * would pick the
     * classes {@code SomeClass} and {@code SomeOtherClass} and derive the {@link ArchModules} from their packages, which
     * in turn would put {@code SomeClass} and {@code AnotherClass} in the same {@link ArchModule} derived from {@code SomeClass}.
     * </p>
     *
     * @param rootClassPredicate A {@link Predicate} determining which {@link JavaClass} is a "root class", thus defining a
     *                           {@link ArchModule} by its package
     * @return A fluent API to further customize how to create {@link ArchModules}
     */
    @PublicAPI(usage = ACCESS)
    public static Creator defineByRootClasses(Predicate<? super JavaClass> rootClassPredicate) {
        return defineBy(identifierByRootClass(rootClassPredicate));
    }

    private static IdentifierAssociation identifierByRootClass(Predicate<? super JavaClass> rootClassPredicate) {
        return new IdentifierAssociation() {
            private final Map<String, Identifier> packageToIdentifier = new HashMap<>();

            @Override
            public void init(Collection<JavaClass> allClasses) {
                allClasses.stream().filter(rootClassPredicate).forEach(rootClass -> {
                    packageToIdentifier.keySet().forEach(pkg -> {
                        if (packagesOverlap(pkg, rootClass.getPackageName())) {
                            throw new IllegalArgumentException(String.format(
                                    "modules from root classes would overlap in '%s' and '%s'", pkg, rootClass.getPackageName()));
                        }
                    });
                    packageToIdentifier.put(rootClass.getPackageName(), Identifier.from(rootClass.getPackageName()));
                });
            }

            private boolean packagesOverlap(String firstPackageName, String secondPackageName) {
                return packageContains(firstPackageName, secondPackageName) || packageContains(secondPackageName, firstPackageName);
            }

            private boolean packageContains(String parentPackage, String childPackage) {
                return childPackage.equals(parentPackage) || childPackage.startsWith(parentPackage + ".");
            }

            @Override
            public Identifier associate(JavaClass javaClass) {
                return packageToIdentifier.entrySet().stream()
                        .filter(it -> packageContains(it.getKey(), javaClass.getPackageName()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(Identifier.ignore());
            }
        };
    }

    /**
     * Entrypoint to create {@link ArchModules} by a generic mapping function {@link JavaClass} -> {@link ArchModule.Identifier}.
     * All {@link JavaClass classes} that are mapped to the same {@link ArchModule.Identifier} will end up in the same
     * {@link ArchModule}.<br>
     *
     * A simple example would be the {@code identifierFunction}
     * <pre><code>
     * javaClass -> ArchModule.Identifier.from(javaClass.getPackageName())
     * </code></pre>
     * This would then create one {@link ArchModule} for each full package name and each {@link JavaClass} would
     * be contained in the {@link ArchModule} where the {@link ArchModule.Identifier} coincides with the class's full package name.
     *
     * @param identifierFunction A function defining how each {@link JavaClass} is mapped to the respective {@link ArchModule.Identifier}
     * @return A fluent API to further customize how to create {@link ArchModules}
     */
    @PublicAPI(usage = ACCESS)
    public static Creator defineBy(IdentifierAssociation identifierFunction) {
        return new Creator(checkNotNull(identifierFunction));
    }

    /**
     * Defines which {@link JavaClass classes} belong to the same {@link ArchModule.Identifier} and thus will eventually
     * end up in the same {@link ArchModule}.
     */
    @FunctionalInterface
    @PublicAPI(usage = INHERITANCE)
    public interface IdentifierAssociation {
        /**
         * An optional hook to add custom logic considering all {@link JavaClass classes} that will be associated with an
         * {@link ArchModule.Identifier}, before {@link #associate(JavaClass)} will be called on any of these {@link JavaClass classes}.
         */
        default void init(Collection<JavaClass> allClasses) {
        }

        /**
         * Associates a {@link JavaClass} with a specific {@link ArchModule.Identifier} which will eventually put this
         * {@link JavaClass} into the {@link ArchModule} with the respective {@link ArchModule.Identifier}.
         * @param javaClass The {@link JavaClass} to associate with an {@link ArchModule.Identifier}
         * @return The associated {@link ArchModule.Identifier}
         */
        Identifier associate(JavaClass javaClass);
    }

    /**
     * An element of the fluent API to create {@link ArchModules}
     */
    @PublicAPI(usage = ACCESS)
    public static final class Creator {
        private final IdentifierAssociation identifierAssociation;
        private final Function<Identifier, String> deriveNameFunction;

        private Creator(IdentifierAssociation identifierAssociation) {
            this(identifierAssociation, DEFAULT_NAMING_STRATEGY);
        }

        private Creator(IdentifierAssociation identifierAssociation, Function<Identifier, String> deriveNameFunction) {
            this.identifierAssociation = checkNotNull(identifierAssociation);
            this.deriveNameFunction = checkNotNull(deriveNameFunction);
        }

        /**
         * Allows to customize each {@link ArchModule} {@link ArchModule#getName() name} by specifying a string pattern
         * that defines how to derive the name from the {@link ArchModule.Identifier}.<br>
         * In particular, the passed {@code namingPattern} may contain numbered placeholders like <code>${1}</code>
         * to refer to parts from the {@link ArchModule.Identifier}. It may also contain the special placeholder
         * <code>$@</code> to refer to the colon-joined form of the identifier.<br>
         * Suppose the {@link ArchModule.Identifier} is {@code ["customer", "creation"]}, then the name could be derived as
         * <pre><code>
         * "Module[${1}/${2}]" -> would yield the name "Module[customer/creation]"
         * "Module[$@]         -> would yield the name "Module[customer:creation]"
         * </code></pre>
         * Note that the derived name must be unique between all {@link ArchModule modules}.
         *
         * @param namingPattern A string naming pattern deriving the {@link ArchModule} {@link ArchModule#getName() name} from
         *                      the {@link ArchModule.Identifier}
         * @return A fluent API to further customize how to create {@link ArchModules}
         */
        @PublicAPI(usage = ACCESS)
        public Creator deriveNameFromPattern(final String namingPattern) {
            return new Creator(identifierAssociation, identifier -> {
                String result = namingPattern;
                for (int i = 1; i <= identifier.getNumberOfParts(); i++) {
                    result = result
                            .replace("$" + i, identifier.getPart(i))
                            .replace("${" + i + "}", identifier.getPart(i))
                            .replace("$@", joinIdentifier(identifier));
                }
                return result;
            });
        }

        /**
         * Derives {@link ArchModules} from the passed {@link JavaClasses} via the specified modularization strategy
         * by the fluent API (e.g. by package identifier or by generic mapping function). In particular,
         * the passed {@link JavaClasses} will be partitioned and sorted into matching instances of {@link ArchModule}.
         *
         * @param classes The classes to modularize
         * @return An instance of {@link ArchModules} containing individual {@link ArchModule}s which in turn contain the partitioned classes
         */
        @PublicAPI(usage = ACCESS)
        public ArchModules modularize(JavaClasses classes) {
            SetMultimap<Identifier, JavaClass> classesByIdentifier = groupClassesByIdentifier(classes);

            Set<ArchModule> modules = new HashSet<>();
            for (Map.Entry<Identifier, Set<JavaClass>> entry : asMap(classesByIdentifier).entrySet()) {
                Identifier identifier = entry.getKey();
                Set<JavaClass> containedClasses = entry.getValue();
                String name = deriveNameFunction.apply(identifier);
                modules.add(new ArchModule(identifier, name, containedClasses));
            }
            return new ArchModules(modules);
        }

        private SetMultimap<Identifier, JavaClass> groupClassesByIdentifier(JavaClasses classes) {
            identifierAssociation.init(classes);

            SetMultimap<Identifier, JavaClass> classesByIdentifier = HashMultimap.create();
            for (JavaClass javaClass : classes) {
                Identifier identifier = identifierAssociation.associate(javaClass);
                if (identifier.shouldBeConsidered()) {
                    classesByIdentifier.put(identifier, javaClass);
                }
            }
            return classesByIdentifier;
        }

        private static final Function<Identifier, String> DEFAULT_NAMING_STRATEGY =
                identifier -> String.format("Module [%s]", joinIdentifier(identifier));

        private static String joinIdentifier(Identifier identifier) {
            return Joiner.on(":").join(identifier);
        }
    }
}
