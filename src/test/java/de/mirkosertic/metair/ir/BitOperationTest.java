package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class BitOperationTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final BitOperation bit = new BitOperation(ConstantDescs.CD_int, BitOperation.Operation.AND, a, b);

        assertThat(bit.debugDescription()).isEqualTo("BitOperation : AND(int)");

        assertThat(bit.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(bit);
        assertThat(b.usedBy).containsExactly(bit);
        assertThat(bit.uses.size()).isEqualTo(2);
        assertThat(bit.uses.get(0).node()).isSameAs(a);
        assertThat(bit.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(bit.uses.get(1).node()).isSameAs(b);
        assertThat(bit.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(bit.isConstant()).isFalse();
    }
}