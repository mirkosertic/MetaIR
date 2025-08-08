package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class AddTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Add add = new Add(ConstantDescs.CD_int, a, b);

        assertThat(add.debugDescription()).isEqualTo("Add : int");

        assertThat(add.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(add);
        assertThat(b.usedBy).containsExactly(add);
        assertThat(add.uses.size()).isEqualTo(2);
        assertThat(add.uses.get(0).node()).isSameAs(a);
        assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(add.uses.get(1).node()).isSameAs(b);
        assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(add.isConstant()).isFalse();
    }

    @Test
    public void fail_arg1_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Add(ConstantDescs.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot add non int value long for arg1");
    }

    @Test
    public void fail_arg2_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Add(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot add non int value long for arg2");
    }

}