package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DFSTest {

    @Test
    public void testOrderWithoutBackEdges() {
        final LabelNode a = new LabelNode("a");
        final LabelNode b = new LabelNode("b");
        final LabelNode c = new LabelNode("c");

        a.controlFlowsTo(b, ControlType.FORWARD);
        b.controlFlowsTo(c, ControlType.FORWARD);

        final DFS dfs = new DFS(a);
        final List<Node> order = dfs.getTopologicalOrder();

        assertThat(order).containsExactly(a, b, c);
    }

    @Test
    public void testOrderWithBackEdges() {
        final LabelNode a = new LabelNode("a");
        final LabelNode b = new LabelNode("b");
        final LabelNode c = new LabelNode("c");

        a.controlFlowsTo(b, ControlType.FORWARD);
        b.controlFlowsTo(c, ControlType.FORWARD);
        b.controlFlowsTo(a, ControlType.BACKWARD);

        final DFS dfs = new DFS(a);
        final List<Node> order = dfs.getTopologicalOrder();

        assertThat(order).containsExactly(a, b, c);
    }
}