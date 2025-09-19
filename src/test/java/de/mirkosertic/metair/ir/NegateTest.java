package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class NegateTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final Negate negate = new Negate(IRType.CD_int, a);

        assertThat(negate.debugDescription()).isEqualTo("Negate : int");

        assertThat(negate.sideeffectFree()).isTrue();

        assertThat(negate.type).isEqualTo(IRType.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(negate);
        assertThat(negate.uses.size()).isEqualTo(1);
        assertThat(negate.uses.getFirst().node()).isSameAs(a);
        assertThat(negate.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(MetaIRTestHelper.toDebugExpression(negate)).isEqualTo("(-10)");
    }

    @Test
    public void fail_wrongtype() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new Negate(IRType.CD_int, new StringConstant("hello"));
            fail("Exception expected");
        }).withMessage("Cannot negate non int of type String");
    }
}