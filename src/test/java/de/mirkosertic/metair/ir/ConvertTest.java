package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ConvertTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final Convert convert = new Convert(ConstantDescs.CD_byte, a, ConstantDescs.CD_int);

        assertThat(convert.debugDescription()).isEqualTo("Convert : int to byte");

        assertThat(convert.type).isEqualTo(ConstantDescs.CD_byte);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(convert);
        assertThat(convert.uses.size()).isEqualTo(1);
        assertThat(convert.uses.getFirst().node()).isSameAs(a);
        assertThat(convert.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }
}