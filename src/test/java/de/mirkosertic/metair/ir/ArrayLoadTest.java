package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayLoadTest {

    @Test
    public void testUsage() {
        final Value a = new NewArray(ConstantDescs.CD_byte.arrayType(), new PrimitiveInt(10));
        final Value index = new PrimitiveInt(0);
        final ArrayLoad load = new ArrayLoad(ConstantDescs.CD_byte.arrayType(), ConstantDescs.CD_byte, a, index);

        assertThat(load.debugDescription()).isEqualTo("ArrayLoad : byte[]");

        assertThat(load).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(load);
        assertThat(index.usedBy).containsExactly(load);
        assertThat(load.uses.size()).isEqualTo(2);
        assertThat(load.uses.get(0).node).isSameAs(a);
        assertThat(load.uses.get(0).use).isEqualTo(new ArgumentUse(0));
        assertThat(load.uses.get(1).node).isSameAs(index);
        assertThat(load.uses.get(1).use).isEqualTo(new ArgumentUse(1));
        assertThat(load.isConstant()).isFalse();

        assertThat(load.peepholeOptimization()).isEmpty();
    }
}