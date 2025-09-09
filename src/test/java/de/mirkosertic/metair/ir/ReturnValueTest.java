package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ReturnValueTest {

    @Test
    public void testUsage() {
        final PrimitiveInt iv = new PrimitiveInt(10);
        final ReturnValue ret = new ReturnValue(ConstantDescs.CD_int, iv);

        assertThat(ret.value).isSameAs(iv);

        assertThat(ret.debugDescription()).isEqualTo("ReturnValue : int");
        assertThat(ret.uses.size()).isEqualTo(1);
        assertThat(ret.uses.getFirst().node()).isSameAs(iv);
        assertThat(ret.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(ret.usedBy).isEmpty();
        assertThat(ret.isConstant()).isFalse();
    }

    @Test
    public void fail_wrong_type_object() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ReturnValue(ConstantDescs.CD_Object, new PrimitiveInt(1));
            fail("Exception expected");
        }).withMessage("Expecting type Object as value, got int");
    }

    @Test
    public void fail_wrong_type_primitive() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ReturnValue(ConstantDescs.CD_int, new StringConstant("Hello"));
            fail("Exception expected");
        }).withMessage("Expecting type int as value, got String");
    }
}