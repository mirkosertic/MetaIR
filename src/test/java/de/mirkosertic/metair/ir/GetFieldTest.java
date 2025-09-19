package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class GetFieldTest {

    @Test
    public void testUsage() {
        final Value v = new StringConstant("Hello");
        final GetField get = new GetField(IRType.CD_String, IRType.CD_int, "field", v);

        assertThat(v.usedBy).containsExactly(get);

        assertThat(get.uses).hasSize(1);
        assertThat(get.uses.getFirst().node()).isSameAs(v);
        assertThat(get.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(get.debugDescription()).isEqualTo("GetField : field : int");
        assertThat(get.isConstant()).isFalse();

        assertThat(MetaIRTestHelper.toDebugExpression(get)).isEqualTo("(\"Hello\".field)");
    }

    @Test
    public void fail_get_on_primitive() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new GetField(IRType.CD_String, IRType.CD_int, "fieldname", new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot get field fieldname from non object source int");
    }
}