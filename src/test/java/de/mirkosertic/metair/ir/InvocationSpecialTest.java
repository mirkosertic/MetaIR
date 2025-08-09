package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class InvocationSpecialTest {

    @Test
    public void testUsage() {
        final Value target = new StringConstant("hello");
        final InvocationSpecial a = new InvocationSpecial(ConstantDescs.CD_String, target, "bar", MethodTypeDesc.of(ConstantDescs.CD_void, List.of(ConstantDescs.CD_int)), List.of(new PrimitiveInt(10)));

        assertThat(a.type).isEqualTo(ConstantDescs.CD_void);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(2);
        assertThat(a.uses.getFirst().node()).isSameAs(target);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.isConstant()).isFalse();

        assertThat(a.debugDescription()).isEqualTo("Invoke special bar : (int)void");
    }

    @Test
    public void fail_invalid_target() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new InvocationSpecial(ConstantDescs.CD_String, new PrimitiveInt(10), "bar", MethodTypeDesc.of(ConstantDescs.CD_void), new ArrayList<>());
            fail("Exception expected");
        }).withMessage("Cannot invoke a method on a primitive value");
    }

    @Test
    public void fail_argumentcount_1() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new InvocationSpecial(ConstantDescs.CD_String, new StringConstant("hello"), "bar", MethodTypeDesc.of(ConstantDescs.CD_void), List.of(new PrimitiveInt(10)));
            fail("Exception expected");
        }).withMessage("Wrong number of arguments for method bar : 0 expected, but got 1");
    }

    @Test
    public void fail_argumentcount_2() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new InvocationSpecial(ConstantDescs.CD_String, new StringConstant("hello"), "bar", MethodTypeDesc.of(ConstantDescs.CD_void, List.of(ConstantDescs.CD_int)), List.of());
            fail("Exception expected");
        }).withMessage("Wrong number of arguments for method bar : 1 expected, but got 0");
    }

    @Test
    public void fail_parametertype_mismatch() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new InvocationSpecial(ConstantDescs.CD_String, new StringConstant("hello"), "bar", MethodTypeDesc.of(ConstantDescs.CD_void, List.of(ConstantDescs.CD_int)), List.of(new PrimitiveLong(10L)));
            fail("Exception expected");
        }).withMessage("Parameter 0 of method bar is a int type, but got long");
    }
}