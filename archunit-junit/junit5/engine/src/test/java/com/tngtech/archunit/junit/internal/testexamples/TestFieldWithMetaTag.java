package com.tngtech.archunit.junit.internal.testexamples;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AnalyzeClasses
public class TestFieldWithMetaTag {
    public static final String FIELD_WITH_META_TAG_NAME = "field_with_meta_tag";

    @MetaTag
    @ArchTest
    static ArchRule field_with_meta_tag = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    @Inherited
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD})
    @ArchTag("field-meta-tag-one")
    @ArchTag("field-meta-tag-two")
    private @interface MetaTag {
    }
}
