package de.mirkosertic.metair.ir;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

public class AddTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Add add = new Add(Type.INT_TYPE, a, b);

        assertThat(add.debugDescription()).isEqualTo("Add : I");

        assertThat(add.type).isEqualTo(Type.INT_TYPE);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(add);
        assertThat(b.usedBy).containsExactly(add);
        assertThat(add.uses.size()).isEqualTo(2);
        assertThat(add.uses.get(0).node).isSameAs(a);
        assertThat(add.uses.get(0).use).isEqualTo(new ArgumentUse(0));
        assertThat(add.uses.get(1).node).isSameAs(b);
        assertThat(add.uses.get(1).use).isEqualTo(new ArgumentUse(1));
        assertThat(add.isConstant()).isFalse();

        assertThat(add.peepholeOptimization()).isEmpty();
    }
}