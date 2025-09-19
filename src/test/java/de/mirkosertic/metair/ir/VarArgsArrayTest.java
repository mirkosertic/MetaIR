package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VarArgsArrayTest {

    @Test
    public void testUsage() {
        final Value elem = new PrimitiveInt(10);
        final Value a = new VarArgsArray(IRType.CD_byte, List.of(elem));

        assertThat(a.arguments()).containsExactly(elem);

        assertThat(a.debugDescription()).isEqualTo("VarArgsArray : byte[]");

        assertThat(a.type).isEqualTo(IRType.CD_byte.arrayType());

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(elem.usedBy).containsExactly(a);
        assertThat(a.uses.size()).isEqualTo(1);
        assertThat(a.uses.getFirst().node()).isSameAs(elem);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("(new byte{10})");
    }
}