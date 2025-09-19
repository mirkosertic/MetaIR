package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveFloatTest {

    @Test
    public void testUsage() {
        final PrimitiveFloat a = new PrimitiveFloat(10.0f);

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.type).isEqualTo(IRType.CD_float);
        assertThat(a.value).isEqualTo(10.0f);
        assertThat(a).isInstanceOf(PrimitiveValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("float 10.0");

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("10.0");
    }

}