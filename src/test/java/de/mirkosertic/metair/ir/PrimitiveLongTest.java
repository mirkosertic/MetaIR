package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveLongTest {

    @Test
    public void testUsage() {
        final PrimitiveLong a = new PrimitiveLong(10L);

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.type).isEqualTo(IRType.CD_long);
        assertThat(a.value).isEqualTo(10L);
        assertThat(a).isInstanceOf(PrimitiveValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("long 10");

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("10");
    }

}