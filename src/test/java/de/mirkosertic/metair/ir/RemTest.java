package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class RemTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Rem rem = new Rem(ConstantDescs.CD_int, a, b);

        assertThat(rem.arg1).isSameAs(a);
        assertThat(rem.arg2).isSameAs(b);

        assertThat(rem.debugDescription()).isEqualTo("Rem : int");

        assertThat(rem.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(rem);
        assertThat(b.usedBy).containsExactly(rem);
        assertThat(rem.uses.size()).isEqualTo(2);
        assertThat(rem.uses.get(0).node()).isSameAs(a);
        assertThat(rem.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(rem.uses.get(1).node()).isSameAs(b);
        assertThat(rem.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(rem.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(rem)).isEqualTo("(10 % 20)");
    }

    @Test
    public void fail_arg1_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Rem(ConstantDescs.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot make remainder non int value long for arg1");
    }

    @Test
    public void fail_arg2_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Rem(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot make remainder non int value long for arg2");
    }
}