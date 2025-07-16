package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class CompareTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Compare compare = new Compare(Compare.Operation.GE, a, b);

        assertThat(compare.debugDescription()).isEqualTo("Compare : GE");

        assertThat(compare.type).isEqualTo(Type.BOOLEAN_TYPE);
        assertThat(compare.operation).isEqualTo(Compare.Operation.GE);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(compare);
        assertThat(b.usedBy).containsExactly(compare);
        assertThat(compare.uses.size()).isEqualTo(2);
        assertThat(compare.uses.get(0).node).isSameAs(a);
        assertThat(compare.uses.get(0).use).isEqualTo(new ArgumentUse(0));
        assertThat(compare.uses.get(1).node).isSameAs(b);
        assertThat(compare.uses.get(1).use).isEqualTo(new ArgumentUse(1));
        assertThat(compare.isConstant()).isFalse();

        assertThat(compare.peepholeOptimization()).isEmpty();
    }

}