package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ArrayLoadTest {

    @Test
    public void testUsage() {
        final Value a = new NewArray(ConstantDescs.CD_byte, new PrimitiveInt(10));
        final Value index = new PrimitiveInt(0);
        final ArrayLoad load = new ArrayLoad(ConstantDescs.CD_byte.arrayType(), a, index);

        assertThat(load.debugDescription()).isEqualTo("ArrayLoad : byte");

        assertThat(load).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(load);
        assertThat(index.usedBy).containsExactly(load);
        assertThat(load.uses.size()).isEqualTo(2);
        assertThat(load.uses.get(0).node()).isSameAs(a);
        assertThat(load.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(load.uses.get(1).node()).isSameAs(index);
        assertThat(load.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(load.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(load)).isEqualTo("((new byte[10])[0])");
    }

    @Test
    public void fail_wrong_array() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ArrayLoad(ConstantDescs.CD_int.arrayType(), new PrimitiveInt(10), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot store to non array of type int");
    }

    @Test
    public void fail_wrong_index() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ArrayLoad(ConstantDescs.CD_int.arrayType(), new NewArray(ConstantDescs.CD_int.arrayType(), new PrimitiveInt(10)), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot store to non int index of type long");
    }
}