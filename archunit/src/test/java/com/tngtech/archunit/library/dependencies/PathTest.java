package com.tngtech.archunit.library.dependencies;

import org.junit.Test;

import java.util.List;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static com.tngtech.archunit.library.dependencies.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathTest {

    @Test
    public void rejects_invalid_edges() {
        List<Edge<String, String>> edges = asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new Path<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Edges are not connected");
    }
}
