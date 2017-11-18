package com.tngtech.archunit.junit;

public class ExpectedLocation {
    public static Creator javaClass(Class<?> clazz) {
        return new Creator(clazz);
    }

    public static class Creator {
        private final Class<?> clazz;

        private Creator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedMessage notResidingIn(String packageIdentifier) {
            String expectedMessage = String.format(
                    "Class %s doesn't reside in a package '%s'", clazz.getName(), packageIdentifier);
            return new ExpectedMessage(expectedMessage);
        }
    }
}
