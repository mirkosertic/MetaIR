package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveByteTest {

    @Test
    public void testUsage() {
        final PrimitiveByte a = new PrimitiveByte(10);

        assertThat(a.type).isEqualTo(Type.BYTE_TYPE);
        assertThat(a.value).isEqualTo(10);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("byte 10");

        assertThat(a.peepholeOptimization()).isEmpty();
    }

}