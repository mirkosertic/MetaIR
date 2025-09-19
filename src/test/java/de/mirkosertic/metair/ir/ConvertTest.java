package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class ConvertTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final Convert convert = new Convert(IRType.CD_byte, a, IRType.CD_int);

        assertThat(convert.debugDescription()).isEqualTo("Convert : int to byte");
        assertThat(convert.sideeffectFree()).isTrue();

        assertThat(convert.type).isEqualTo(IRType.CD_byte);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(convert);
        assertThat(convert.uses.size()).isEqualTo(1);
        assertThat(convert.uses.getFirst().node()).isSameAs(a);
        assertThat(convert.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(MetaIRTestHelper.toDebugExpression(convert)).isEqualTo("((byte)10)");
    }

    @Test
    public void fail_arg1_wrong() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Convert(IRType.CD_int, new PrimitiveInt(10), IRType.CD_long);
            fail("Exception expected");
        }).withMessage("Expected a value of type long but got int");
    }
}