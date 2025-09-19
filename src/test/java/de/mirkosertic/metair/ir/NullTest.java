package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullTest {

    @Test
    public void testUsage() {
        final Null a = new Null();

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.sideeffectFree()).isTrue();

        assertThat(a.type).isEqualTo(IRType.CD_Object);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("null");

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("null");
    }

}