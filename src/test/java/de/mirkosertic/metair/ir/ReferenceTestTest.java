package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceTestTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("10");
        final ReferenceTest referenceCondition = new ReferenceTest(ReferenceTest.Operation.NONNULL, a);

        assertThat(referenceCondition.debugDescription()).isEqualTo("ReferenceTest : NONNULL");

        assertThat(referenceCondition.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(referenceCondition.operation).isEqualTo(ReferenceTest.Operation.NONNULL);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(referenceCondition);
        assertThat(referenceCondition.uses.size()).isEqualTo(1);
        assertThat(referenceCondition.uses.getFirst().node()).isSameAs(a);
        assertThat(referenceCondition.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(referenceCondition.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(referenceCondition)).isEqualTo("(\"10\" != null)");
    }
}