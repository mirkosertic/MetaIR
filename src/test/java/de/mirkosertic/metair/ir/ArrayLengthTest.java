package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayLengthTest {

    @Test
    public void testUsage() {
        final Value a = new NewArray(ConstantDescs.CD_byte.arrayType(), new PrimitiveInt(10));
        final ArrayLength len = new ArrayLength(a);

        assertThat(len.debugDescription()).isEqualTo("ArrayLength");

        assertThat(len).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(len);
        assertThat(len.uses.size()).isEqualTo(1);
        assertThat(len.uses.getFirst().node).isSameAs(a);
        assertThat(len.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(len.isConstant()).isFalse();

        assertThat(len.peepholeOptimization()).isEmpty();
    }
}