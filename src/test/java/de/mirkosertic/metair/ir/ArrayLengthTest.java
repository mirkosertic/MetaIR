package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ArrayLengthTest {

    @Test
    public void testUsage() {
        final Value a = new NewArray(IRType.CD_byte, new PrimitiveInt(10));
        final ArrayLength len = new ArrayLength(a);

        assertThat(len.debugDescription()).isEqualTo("ArrayLength");

        assertThat(len).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(len);
        assertThat(len.uses.size()).isEqualTo(1);
        assertThat(len.uses.getFirst().node()).isSameAs(a);
        assertThat(len.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(len.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(len)).isEqualTo("(new byte[10]).length");
    }

    @Test
    public void fail_wrong_type() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new ArrayLength(new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot get array length of non array of type int");
    }
}