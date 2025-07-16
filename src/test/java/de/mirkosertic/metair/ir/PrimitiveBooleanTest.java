package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveBooleanTest {

    @Test
    public void testUsage() {
        final PrimitiveBoolean a = new PrimitiveBoolean(true);

        assertThat(a.type).isEqualTo(Type.BOOLEAN_TYPE);
        assertThat(a.value).isTrue();
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("boolean true");

        assertThat(a.peepholeOptimization()).isEmpty();
    }

}