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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingSet;
import com.tngtech.archunit.base.Suppliers;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.emptyList;

/**
 * Represents a generic "architecture module", i.e. any group of classes that should form a cohesive unit.<br>
 * An {@link ArchModule} can be identified by its {@link #getIdentifier() identifier}. Vice versa and {@link ArchModule}
 * can be defined as a mapping {@code JavaClass -> ArchModule.Identifier}, where all classes that are mapped to the
 * same identifier will end up in the same module.<br>
 * {@link ArchModule} offers an API to obtain all {@link #getClassDependenciesFromSelf() class dependencies}, i.e.
 * all {@link Dependency dependencies} from {@link JavaClass classes} within the module to {@link JavaClass classes}
 * outside of the module. It also offers an API to obtain all {@link #getModuleDependenciesFromSelf() module dependencies},
 * i.e. dependencies from this {@link ArchModule} to another {@link ArchModule} where these dependencies reflect
 * all {@link #getClassDependenciesFromSelf() class dependencies} where the origin resides within this {@link ArchModule}
 * and the target resides within another {@link ArchModule}.
 * <br><br>
 * To create {@link ArchModule}s please refer to {@link ArchModules}.
 */
@PublicAPI(usage = ACCESS)
public final class ArchModule extends ForwardingSet<JavaClass> implements HasName {
    private final Identifier identifier;
    private final ArchModule.Name name;
    private final Set<JavaClass> classes;
    private final Set<Dependency> classDependenciesFromSelf;
    // resolving backwards dependencies is done lazily in JavaClass, so we don't trigger it eagerly here either
    private final Supplier<Set<Dependency>> classDependenciesToSelf;
    private Set<ModuleDependency> moduleDependenciesFromSelf;
    private Set<Dependency> undefinedDependencies;
    private Set<ModuleDependency> moduleDependenciesToSelf;

    ArchModule(Identifier identifier, String name, Set<JavaClass> classes) {
        this.identifier = checkNotNull(identifier);
        this.name = Name.from(name);
        this.classes = ImmutableSet.copyOf(classes);
        classDependenciesFromSelf = classes.stream()
                .flatMap(clazz -> clazz.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> !classes.contains(dependency.getTargetClass()))
                .collect(toImmutableSet());
        classDependenciesToSelf = Suppliers.memoize(() -> classes.stream()
                .flatMap(clazz -> clazz.getDirectDependenciesToSelf().stream())
                .filter(dependency -> !classes.contains(dependency.getOriginClass()))
                .collect(toImmutableSet()));
    }

    void setModuleDependencies(Set<ModuleDependency> moduleDependenciesFromSelf, Set<ModuleDependency> moduleDependenciesToSelf) {
        this.moduleDependenciesFromSelf = ImmutableSet.copyOf(moduleDependenciesFromSelf);
        this.moduleDependenciesToSelf = ImmutableSet.copyOf(moduleDependenciesToSelf);
        this.undefinedDependencies = ImmutableSet.copyOf(Sets.difference(classDependenciesFromSelf, toClassDependencies(moduleDependenciesFromSelf)));
    }

    private Set<Dependency> toClassDependencies(Set<ModuleDependency> moduleDependencies) {
        return moduleDependencies.stream().flatMap(it -> it.toClassDependencies().stream()).collect(toImmutableSet());
    }

    @Override
    protected Set<JavaClass> delegate() {
        return classes;
    }

    /**
     * @return The {@link Identifier} of this module
     */
    @PublicAPI(usage = ACCESS)
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * @return The name of this module, i.e. a human-readable string representing this module
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name.value;
    }

    /**
     * @return All {@link Dependency dependencies} where the {@link Dependency#getOriginClass() origin class}
     *         is contained within this {@link ArchModule}.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesFromSelf() {
        return classDependenciesFromSelf;
    }

    /**
     * @return All {@link Dependency dependencies} where the {@link Dependency#getTargetClass() target class}
     *         is contained within this {@link ArchModule}.
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getClassDependenciesToSelf() {
        return classDependenciesToSelf.get();
    }

    /**
     * @return All {@link ModuleDependency module dependencies} where the {@link ModuleDependency#getOrigin() origin}
     *         equals this {@link ArchModule}.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ModuleDependency> getModuleDependenciesFromSelf() {
        return moduleDependenciesFromSelf;
    }

    /**
     * @return All {@link ModuleDependency module dependencies} where the {@link ModuleDependency#getTarget() target}
     *         equals this {@link ArchModule}.
     */
    @PublicAPI(usage = ACCESS)
    public Set<ModuleDependency> getModuleDependenciesToSelf() {
        return moduleDependenciesToSelf;
    }

    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getUndefinedDependencies() {
        return undefinedDependencies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final ArchModule other = (ArchModule) obj;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier=" + getIdentifier() + ", name=" + getName() + '}';
    }

    /**
     * An {@link Identifier} of an {@link ArchModule}. An {@link Identifier} is basically an ordered list of string parts that
     * uniquely identifies an {@link ArchModule}, i.e. two {@link ArchModule modules} are equal, if and only if
     * their identifier is equal (i.e. all the textual parts of the identifier match in order).
     */
    @PublicAPI(usage = ACCESS)
    public static final class Identifier implements Iterable<String> {
        private final List<String> parts;

        private Identifier(List<String> parts) {
            this.parts = ImmutableList.copyOf(parts);
        }

        /**
         * @see #from(List)
         */
        @PublicAPI(usage = ACCESS)
        public static Identifier from(String... parts) {
            return from(ImmutableList.copyOf(parts));
        }

        /**
         * @param parts The textual parts of the {@link Identifier}, must not be empty
         * @return An {@link Identifier} consisting of the passed {@code parts}
         */
        @PublicAPI(usage = ACCESS)
        public static Identifier from(List<String> parts) {
            checkArgument(!parts.isEmpty(), "Parts may not be empty");
            return new Identifier(parts);
        }

        /**
         * Factory method to signal that this {@link ArchModule} is irrelevant.
         * The {@link ArchModule} identified by this {@link Identifier} will e.g. be omitted when creating {@link ArchModules}
         * and should i.g. be completely ignored for all purposes.
         *
         * @return An {@link Identifier} that signals that this {@link ArchModule} is irrelevant and should be ignored.
         */
        @PublicAPI(usage = ACCESS)
        public static Identifier ignore() {
            return new Identifier(emptyList());
        }

        /**
         * @return The number of (textual) parts this identifier consists of.
         */
        @PublicAPI(usage = ACCESS)
        public int getNumberOfParts() {
            return parts.size();
        }

        /**
         * @param index Index of the requested (textual) part
         * @return Part with the given index; indizes are 1-based (i.e. {@link #getPart(int) getPart(1)}) returns the first part.
         */
        @PublicAPI(usage = ACCESS)
        public String getPart(int index) {
            checkArgument(index >= 1 && index <= parts.size(), "Index %d is out of bounds", index);
            return parts.get(index - 1);
        }

        boolean shouldBeConsidered() {
            return !parts.isEmpty();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public Iterator<String> iterator() {
            return parts.iterator();
        }

        @Override
        public int hashCode() {
            return Objects.hash(parts);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Identifier other = (Identifier) obj;
            return Objects.equals(this.parts, other.parts);
        }

        @Override
        public String toString() {
            return parts.toString();
        }
    }

    private static final class Name {
        private final String value;

        private Name(String value) {
            checkArgument(!isNullOrEmpty(value), "Module name must not be null or empty");
            this.value = value;
        }

        static Name from(String value) {
            return new Name(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Name other = (Name) obj;
            return Objects.equals(this.value, other.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
