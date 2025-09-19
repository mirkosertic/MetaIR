package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class NewArrayTest {

    @Test
    public void testUsage() {
        final Value size = new PrimitiveInt(10);
        final NewArray a = new NewArray(IRType.CD_byte, size);

        assertThat(a.debugDescription()).isEqualTo("NewArray : byte[]");

        assertThat(a.type).isEqualTo(IRType.CD_byte.arrayType());

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(size.usedBy).containsExactly(a);
        assertThat(a.uses.size()).isEqualTo(1);
        assertThat(a.uses.getFirst().node()).isSameAs(size);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }

    @Test
    public void fail_invalidlength() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new NewArray(IRType.CD_byte, new PrimitiveDouble(10));
            fail("Exception expected");
        }).withMessage("Array length must be int, but was double");
    }
}