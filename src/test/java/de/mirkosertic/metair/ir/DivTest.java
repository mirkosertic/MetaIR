package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class DivTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Div div = new Div(ConstantDescs.CD_int, a, b);

        assertThat(div.arg1).isSameAs(a);
        assertThat(div.arg2).isSameAs(b);

        assertThat(div.debugDescription()).isEqualTo("Div : int");

        assertThat(div.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(div);
        assertThat(b.usedBy).containsExactly(div);
        assertThat(div.uses.size()).isEqualTo(2);
        assertThat(div.uses.get(0).node()).isSameAs(a);
        assertThat(div.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(div.uses.get(1).node()).isSameAs(b);
        assertThat(div.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(div.isConstant()).isFalse();
    }

    @Test
    public void fail_arg1_wrongtype() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Div(ConstantDescs.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot divide non int value long for arg1");
    }

    @Test
    public void fail_arg2_wrongtype() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Div(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot divide non int value long for arg2");
    }
}