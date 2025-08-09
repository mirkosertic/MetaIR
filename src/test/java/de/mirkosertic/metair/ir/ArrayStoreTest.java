package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ArrayStoreTest {

    @Test
    public void testUsage() {
        final Value a = new NewArray(ConstantDescs.CD_byte, new PrimitiveInt(10));
        final Value index = new PrimitiveInt(0);
        final Value value = new PrimitiveInt(42);
        final ArrayStore store = new ArrayStore(ConstantDescs.CD_byte.arrayType(), a, index, value);

        assertThat(store.debugDescription()).isEqualTo("ArrayStore : byte[]");

        assertThat(a.usedBy).containsExactly(store);
        assertThat(index.usedBy).containsExactly(store);
        assertThat(value.usedBy).containsExactly(store);
        assertThat(store.uses.size()).isEqualTo(3);
        assertThat(store.uses.get(0).node()).isSameAs(a);
        assertThat(store.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(store.uses.get(1).node()).isSameAs(index);
        assertThat(store.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(store.uses.get(2).node()).isSameAs(value);
        assertThat(store.uses.get(2).use()).isEqualTo(new ArgumentUse(2));
        assertThat(store.isConstant()).isFalse();

        assertThat(store.debugDescription()).isEqualTo("ArrayStore : byte[]");
    }

    @Test
    public void fail_wrong_array() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ArrayStore(ConstantDescs.CD_byte.arrayType(), new PrimitiveInt(10), new PrimitiveInt(10), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot store to non array of type int");
    }

    @Test
    public void fail_wrong_index() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ArrayStore(ConstantDescs.CD_byte.arrayType(), new NewArray(ConstantDescs.CD_int, new PrimitiveInt(10)), new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot store to non int index of type long");
    }
}