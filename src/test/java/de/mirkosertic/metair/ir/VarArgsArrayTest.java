package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VarArgsArrayTest {

    @Test
    public void testUsage() {
        final Value size = new PrimitiveInt(10);
        final Value a = new VarArgsArray(ConstantDescs.CD_byte, List.of(size));

        assertThat(a.debugDescription()).isEqualTo("VarArgsArray : byte[]");

        assertThat(a.type).isEqualTo(ConstantDescs.CD_byte.arrayType());

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(size.usedBy).containsExactly(a);
        assertThat(a.uses.size()).isEqualTo(1);
        assertThat(a.uses.getFirst().node()).isSameAs(size);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }
}