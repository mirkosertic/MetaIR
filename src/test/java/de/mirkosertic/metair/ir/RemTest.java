package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class RemTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Rem rem = new Rem(ConstantDescs.CD_int, a, b);

        assertThat(rem.debugDescription()).isEqualTo("Rem : int");

        assertThat(rem.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(rem);
        assertThat(b.usedBy).containsExactly(rem);
        assertThat(rem.uses.size()).isEqualTo(2);
        assertThat(rem.uses.get(0).node()).isSameAs(a);
        assertThat(rem.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(rem.uses.get(1).node()).isSameAs(b);
        assertThat(rem.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(rem.isConstant()).isFalse();
    }
}