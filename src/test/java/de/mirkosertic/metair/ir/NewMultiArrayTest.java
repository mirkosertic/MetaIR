package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class NewMultiArrayTest {

    @Test
    public void testUsage() {
        final Value size = new PrimitiveInt(10);
        final Value a = new NewMultiArray(ConstantDescs.CD_byte.arrayType(), List.of(size));

        assertThat(a.debugDescription()).isEqualTo("NewMultiArray : byte[]");

        assertThat(a.type).isEqualTo(ConstantDescs.CD_byte.arrayType());

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(size.usedBy).containsExactly(a);
        assertThat(a.uses.size()).isEqualTo(1);
        assertThat(a.uses.getFirst().node()).isSameAs(size);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }

    @Test
    public void fail_wrong_length() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new NewMultiArray(ConstantDescs.CD_int.arrayType(), List.of(new PrimitiveLong(10)));
            fail("Exception expected");
        }).withMessage("Array dimension must be int, but was long for dimension 1");
    }
}