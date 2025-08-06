package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class NegateTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final Negate negate = new Negate(ConstantDescs.CD_int, a);

        assertThat(negate.debugDescription()).isEqualTo("Negate : int");

        assertThat(negate.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(negate);
        assertThat(negate.uses.size()).isEqualTo(1);
        assertThat(negate.uses.getFirst().node()).isSameAs(a);
        assertThat(negate.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
    }
}