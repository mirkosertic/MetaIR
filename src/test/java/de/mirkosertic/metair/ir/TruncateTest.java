package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

class TruncateTest {

    @Test
    public void testUsage() {
        final Value v = new PrimitiveInt(10);
        final Truncate a = new Truncate(ConstantDescs.CD_char, v);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_char);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node()).isSameAs(v);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.debugDescription()).isEqualTo("Truncate : char");
    }
}