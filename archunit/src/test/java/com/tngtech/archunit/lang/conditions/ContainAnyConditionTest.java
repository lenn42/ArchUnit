package com.tngtech.archunit.lang.conditions;

import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containAnyElementThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ContainsOnlyConditionTest.IS_SERIALIZABLE;
import static com.tngtech.archunit.lang.conditions.ContainsOnlyConditionTest.ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT;
import static com.tngtech.archunit.lang.conditions.ContainsOnlyConditionTest.SerializableObject;
import static com.tngtech.archunit.lang.conditions.ContainsOnlyConditionTest.isSerializableMessageFor;
import static com.tngtech.archunit.lang.conditions.ContainsOnlyConditionTest.messageForTwoTimes;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ContainAnyConditionTest {
    private static final List<Object> TWO_NONSERIALIZABLE_OBJECTS = asList(new Object(), new Object());

    @Test
    public void satisfied_works_and_description_contains_mismatches() {
        ConditionEvents events = ConditionEvents.Factory.create();
        containAnyElementThat(IS_SERIALIZABLE).check(TWO_NONSERIALIZABLE_OBJECTS, events);
        assertThat(events).containViolations(messageForTwoTimes(isSerializableMessageFor(Object.class)));

        events = ConditionEvents.Factory.create();
        containAnyElementThat(IS_SERIALIZABLE).check(ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT, events);
        assertThat(events).containNoViolation();
    }

    @Test
    public void inverting_works() {
        ConditionEvents events = ConditionEvents.Factory.create();
        ArchCondition<Collection<?>> condition = containAnyElementThat(IS_SERIALIZABLE);

        condition.check(ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT, events);
        assertThat(events).containNoViolation();

        events = ConditionEvents.Factory.create();
        never(containAnyElementThat(IS_SERIALIZABLE)).check(ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT, events);
        assertThat(events).containViolations(isSerializableMessageFor(SerializableObject.class));
    }

    @Test
    public void if_there_are_no_input_events_no_ContainsAnyEvent_is_added() {
        ViolatedAndSatisfiedConditionEvents events = new ViolatedAndSatisfiedConditionEvents();
        containOnlyElementsThat(IS_SERIALIZABLE).check(emptyList(), events);
        assertThat(events.getAllowed()).as("allowed events").isEmpty();
        assertThat(events.getViolating()).as("violated events").isEmpty();
    }
}
