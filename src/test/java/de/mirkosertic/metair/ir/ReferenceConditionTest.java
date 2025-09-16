package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceConditionTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("10");
        final StringConstant b = new StringConstant("20");
        final ReferenceCondition referenceCondition = new ReferenceCondition(ReferenceCondition.Operation.EQ, a, b);

        assertThat(referenceCondition.debugDescription()).isEqualTo("ReferenceCondition : EQ");

        assertThat(referenceCondition.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(referenceCondition.operation).isEqualTo(ReferenceCondition.Operation.EQ);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(referenceCondition);
        assertThat(b.usedBy).containsExactly(referenceCondition);
        assertThat(referenceCondition.uses.size()).isEqualTo(2);
        assertThat(referenceCondition.uses.get(0).node()).isSameAs(a);
        assertThat(referenceCondition.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(referenceCondition.uses.get(1).node()).isSameAs(b);
        assertThat(referenceCondition.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(referenceCondition.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(referenceCondition)).isEqualTo("(\"10\" == \"20\")");
    }

}