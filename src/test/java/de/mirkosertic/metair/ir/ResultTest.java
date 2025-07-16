package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {

    @Test
    public void testUsage() {
        final Result a = new Result(Type.INT_TYPE);

        assertThat(a.type).isEqualTo(Type.INT_TYPE);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Result : I");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}