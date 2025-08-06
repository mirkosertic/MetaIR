package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class NumericConditionTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final NumericCondition numericCondition = new NumericCondition(NumericCondition.Operation.GE, a, b);

        assertThat(numericCondition.debugDescription()).isEqualTo("NumericCondition : GE");

        assertThat(numericCondition.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(numericCondition.operation).isEqualTo(NumericCondition.Operation.GE);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(numericCondition);
        assertThat(b.usedBy).containsExactly(numericCondition);
        assertThat(numericCondition.uses.size()).isEqualTo(2);
        assertThat(numericCondition.uses.get(0).node()).isSameAs(a);
        assertThat(numericCondition.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(numericCondition.uses.get(1).node()).isSameAs(b);
        assertThat(numericCondition.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(numericCondition.isConstant()).isFalse();
    }

}