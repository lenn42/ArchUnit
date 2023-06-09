package com.tngtech.archunit.testutil.assertion;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.assertj.core.api.AbstractIterableAssert;

import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;

public class JavaMethodsAssertion
        extends AbstractIterableAssert<JavaMethodsAssertion, Iterable<? extends JavaMethod>, JavaMethod, JavaMethodAssertion> {

    public JavaMethodsAssertion(Iterable<JavaMethod> methods) {
        super(methods, JavaMethodsAssertion.class);
    }

    @Override
    protected JavaMethodAssertion toAssert(JavaMethod value, String description) {
        return new JavaMethodAssertion(value).as(description);
    }

    @Override
    protected JavaMethodsAssertion newAbstractIterableAssert(Iterable<? extends JavaMethod> iterable) {
        return new JavaMethodsAssertion(ImmutableSet.copyOf(iterable));
    }

    public JavaMethodsAssertion contain(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (!contains(owner, name, parameterTypes)) {
            throw new AssertionError(String.format("There is no method %s contained in %s",
                    formatMethod(owner.getName(), name, formatNamesOf(parameterTypes)), actual));
        }
        return this;
    }

    private boolean contains(Class<?> owner, String name, Class<?>[] parameterTypes) {
        for (JavaMethod method : actual) {
            if (method.getOwner().isEquivalentTo(owner) && method.getName().equals(name) && rawParameterTypes(parameterTypes).test(method)) {
                return true;
            }
        }
        return false;
    }
}
