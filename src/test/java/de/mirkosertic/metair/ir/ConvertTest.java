package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ConvertTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final Convert convert = new Convert(ConstantDescs.CD_byte, a, ConstantDescs.CD_int);

        assertThat(convert.debugDescription()).isEqualTo("Convert : int to byte");

        assertThat(convert.type).isEqualTo(ConstantDescs.CD_byte);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(convert);
        assertThat(convert.uses.size()).isEqualTo(1);
        assertThat(convert.uses.getFirst().node()).isSameAs(a);
        assertThat(convert.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }

    @Test
    public void fail_arg1_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Convert(ConstantDescs.CD_int, new PrimitiveInt(10), ConstantDescs.CD_long);
            fail("Exception expected");
        }).withMessage("Expected a value of type long but got int");
    }
}