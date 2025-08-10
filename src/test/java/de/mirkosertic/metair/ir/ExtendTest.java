package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

class ExtendTest {

    @Test
    public void testUsage() {
        final Value v = new PrimitiveInt(10);
        final Extend a = new Extend(ConstantDescs.CD_int, Extend.ExtendType.SIGN, v);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node()).isSameAs(v);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(a.debugDescription()).isEqualTo("Extend : SIGN int");
    }
}