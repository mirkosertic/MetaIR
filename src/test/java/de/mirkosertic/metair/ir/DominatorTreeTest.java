package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DominatorTreeTest {

    @Test
    public void testSimpleAdd() {
        final Method m = new Method();
        final MethodArgument arg = m.defineMethodArgument(ConstantDescs.CD_int, 0);
        final PrimitiveInt i = m.definePrimitiveInt(10);
        final Add add = new Add(ConstantDescs.CD_int, arg, i);
        final ReturnValue rv = new ReturnValue(ConstantDescs.CD_int, add);
        m.controlFlowsTo(rv, ControlType.FORWARD);

        final DominatorTree dt = new DominatorTree(m);
        final List<Node> order = dt.preOrder;
        assertThat(order).containsExactly(m, arg, i, add, rv);
    }
}