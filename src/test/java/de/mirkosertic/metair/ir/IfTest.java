package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IfTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Compare compare = new Compare(Compare.Operation.GE, a, b);
        final If iff = new If(compare);

        assertThat(a.usedBy).containsExactly(compare);
        assertThat(b.usedBy).containsExactly(compare);
        assertThat(iff.uses.size()).isEqualTo(1);
        assertThat(iff.uses.getFirst().node).isSameAs(compare);
        assertThat(iff.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(compare.isConstant()).isFalse();

        assertThat(iff.peepholeOptimization()).isEmpty();
    }
}