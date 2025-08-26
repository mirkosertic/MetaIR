package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DFS2Test {

    @Test
    public void testOrderWithoutBackEdges() {
        final LabelNode a = new LabelNode("a");
        final LabelNode b = new LabelNode("b");
        final LabelNode c = new LabelNode("c");

        a.controlFlowsTo(b, FlowType.FORWARD);
        b.controlFlowsTo(c, FlowType.FORWARD);

        final DFS2 dfs = new DFS2(a);
        final List<Node> order = dfs.getTopologicalOrder();

        assertThat(order).containsExactly(a, b, c);
    }

    @Test
    public void testOrderWithBackEdges() {
        final LabelNode a = new LabelNode("a");
        final LabelNode b = new LabelNode("b");
        final LabelNode c = new LabelNode("c");

        a.controlFlowsTo(b, FlowType.FORWARD);
        b.controlFlowsTo(c, FlowType.FORWARD);
        b.controlFlowsTo(a, FlowType.BACKWARD);

        final DFS2 dfs = new DFS2(a);
        final List<Node> order = dfs.getTopologicalOrder();

        assertThat(order).containsExactly(a, b, c);
    }

    @Test
    public void testSimpleAdd() {
        final Method m = new Method();
        final ExtractMethodArgProjection arg = m.defineMethodArgument(ConstantDescs.CD_int, 0);
        final PrimitiveInt i = m.definePrimitiveInt(10);
        final Add add = new Add(ConstantDescs.CD_int, arg, i);
        final ReturnValue rv = new ReturnValue(ConstantDescs.CD_int, add);
        m.controlFlowsTo(rv, FlowType.FORWARD);

        final DFS2 dfs = new DFS2(m);
        final List<Node> order = dfs.getTopologicalOrder();
        assertThat(order).containsExactly(m, arg, i, add, rv);
    }
}