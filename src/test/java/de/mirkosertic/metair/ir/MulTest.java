package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class MulTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Mul mul = new Mul(ConstantDescs.CD_int, a, b);

        assertThat(mul.debugDescription()).isEqualTo("Mul : int");

        assertThat(mul.sideeffectFree()).isTrue();

        assertThat(mul.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(mul);
        assertThat(b.usedBy).containsExactly(mul);
        assertThat(mul.uses.size()).isEqualTo(2);
        assertThat(mul.uses.get(0).node()).isSameAs(a);
        assertThat(mul.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(mul.uses.get(1).node()).isSameAs(b);
        assertThat(mul.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(mul.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(mul)).isEqualTo("(10 * 20)");
    }

    @Test
    public void fail_arg1_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Mul(ConstantDescs.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot multiply non int value long for arg1");
    }

    @Test
    public void fail_arg2_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Mul(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot multiply non int value long for arg2");
    }
}