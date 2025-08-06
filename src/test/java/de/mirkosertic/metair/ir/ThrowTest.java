package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThrowTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("LOCK");
        final Throw tr = new Throw(a);

        assertThat(tr.debugDescription()).isEqualTo("Throw");

        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(tr);
        assertThat(tr.uses.size()).isEqualTo(1);
        assertThat(tr.uses.getFirst().node()).isSameAs(a);
        assertThat(tr.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(tr.isConstant()).isFalse();
    }
}