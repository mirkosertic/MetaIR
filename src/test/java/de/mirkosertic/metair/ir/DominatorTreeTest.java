package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DominatorTreeTest {

    @Test
    public void testSimpleAdd() {
        final Method m = new Method();
        final ExtractMethodArgProjection arg = m.defineMethodArgument(IRType.CD_int, 0);
        final PrimitiveInt i = m.definePrimitiveInt(10);
        final Add add = new Add(IRType.CD_int, arg, i);
        final ReturnValue rv = new ReturnValue(IRType.CD_int, add);
        m.controlFlowsTo(rv, FlowType.FORWARD);

        final DominatorTree dt = new DominatorTree(m);
        final List<Node> order = dt.preOrder;
        assertThat(order).containsExactly(m, arg, i, add, rv);
    }
}