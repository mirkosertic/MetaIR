package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckCastTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final CheckCast cc = new CheckCast(a, b);

        assertThat(cc.arg0).isSameAs(a);
        assertThat(cc.arg1).isSameAs(b);

        assertThat(cc.debugDescription()).isEqualTo("CheckCast");

        assertThat(a.usedBy).containsExactly(cc);
        assertThat(b.usedBy).containsExactly(cc);
        assertThat(cc.uses.size()).isEqualTo(2);
        assertThat(cc.uses.get(0).node()).isSameAs(a);
        assertThat(cc.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(cc.uses.get(1).node()).isSameAs(b);
        assertThat(cc.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
    }
}