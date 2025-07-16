package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class ReturnValueTest {

    @Test
    public void testUsage() {
        final PrimitiveInt iv = new PrimitiveInt(10);
        final ReturnValue ret = new ReturnValue(Type.INT_TYPE, iv);

        assertThat(ret.debugDescription()).isEqualTo("ReturnValue : I");

        assertThat(ret.uses.size()).isEqualTo(1);
        assertThat(ret.uses.getFirst().node).isSameAs(iv);
        assertThat(ret.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(ret.usedBy).isEmpty();
        assertThat(ret.isConstant()).isTrue();

        assertThat(ret.peepholeOptimization()).isEmpty();
    }
}