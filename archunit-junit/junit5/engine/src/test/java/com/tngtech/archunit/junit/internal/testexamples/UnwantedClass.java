package com.tngtech.archunit.junit.internal.testexamples;

public class UnwantedClass {
    public static final Class<?> CLASS_SATISFYING_RULES = Object.class;
    public static final Class<?> CLASS_VIOLATING_RULES = UnwantedClass.class;
}
