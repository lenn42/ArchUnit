package com.tngtech.archunit.testutil;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.Condition;

import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;

public final class Conditions {
    private Conditions() {
    }

    public static <T> Condition<Iterable<? extends T>> containing(Condition<T> condition) {
        return new Condition<Iterable<? extends T>>() {
            @Override
            public boolean matches(Iterable<? extends T> value) {
                boolean contains = false;
                for (T t : value) {
                    contains = contains || condition.matches(t);
                }
                return contains;
            }
        }.as("containing an element that " + condition.description());
    }

    public static Condition<JavaCodeUnit> codeUnitWithSignature(String name, Class<?>... parameters) {
        return new Condition<JavaCodeUnit>() {
            @Override
            public boolean matches(JavaCodeUnit value) {
                return name.equals(value.getName()) && formatNamesOf(parameters).equals(HasName.Utils.namesOf(value.getRawParameterTypes()));
            }
        }.as("matches signature <" + name + ", " + formatNamesOf(parameters) + ">");
    }
}
