package com.tngtech.archunit.library.dependencies;

import org.junit.Test;

import java.util.List;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static com.tngtech.archunit.library.dependencies.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CycleTest {

    @Test
    public void rejects_invalid_edges() {
        List<Edge<String, String>> edges = asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new Cycle<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Edges are not connected");
    }

    @Test
    public void rejects_single_edge() {
        List<Edge<String, String>> edges = singletonList(stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new Cycle<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not form a cycle");
    }

    @Test
    public void minimal_nontrivial_cycle() {
        String nodeA = "Node-A";
        String nodeB = "Node-B";
        Cycle<String, ?> cycle = new Cycle<>(asList(stringEdge(nodeA, nodeB), stringEdge(nodeB, nodeA)));

        assertThat(cycle.getEdges()).hasSize(2);
    }
}
