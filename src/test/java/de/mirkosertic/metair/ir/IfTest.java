package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IfTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final NumericCondition numericCondition = new NumericCondition(NumericCondition.Operation.GE, a, b);
        final If iff = new If(numericCondition);

        assertThat(a.usedBy).containsExactly(numericCondition);
        assertThat(b.usedBy).containsExactly(numericCondition);
        assertThat(iff.uses.size()).isEqualTo(1);
        assertThat(iff.uses.getFirst().node).isSameAs(numericCondition);
        assertThat(iff.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(numericCondition.isConstant()).isFalse();

        assertThat(iff.peepholeOptimization()).isEmpty();

        assertThat(iff.debugDescription()).isEqualTo("If");
    }
}