package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class SubTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Sub sub = new Sub(ConstantDescs.CD_int, a, b);

        assertThat(sub.debugDescription()).isEqualTo("Sub : int");

        assertThat(sub.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(sub);
        assertThat(b.usedBy).containsExactly(sub);
        assertThat(sub.uses.size()).isEqualTo(2);
        assertThat(sub.uses.get(0).node()).isSameAs(a);
        assertThat(sub.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(sub.uses.get(1).node()).isSameAs(b);
        assertThat(sub.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(sub.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(sub)).isEqualTo("(10 - 20)");
    }

    @Test
    public void fail_wrong_arg1() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Sub(ConstantDescs.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot subtract non int value long for arg1");
    }

    @Test
    public void fail_wrong_arg2() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Sub(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot subtract non int value long for arg2");
    }

}