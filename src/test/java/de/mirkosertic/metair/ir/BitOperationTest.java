package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

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

        assertThat(MetaIRTestHelper.toDebugExpression(bit)).isEqualTo("(10 & 20)");
    }

    @Test
    public void fail_arg1_wrongtype() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new BitOperation(ConstantDescs.CD_int, BitOperation.Operation.OR, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot use non int value long for bit operation OR on arg1");
    }

    @Test
    public void fail_arg2_wrongtype() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new BitOperation(ConstantDescs.CD_int, BitOperation.Operation.OR, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot use non int value long for bit operation OR on arg2");
    }
}