package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;

import static org.assertj.core.api.Assertions.assertThat;

class CatchTest {

    @Test
    public void testUsage() {
        final Method m = new Method();
        final Catch c = new Catch(ClassDesc.of(Exception.class.getName()), m);

        assertThat(c.debugDescription()).isEqualTo("Catch : Exception");
        assertThat(c.usedBy).hasSize(1);
        assertThat(c.uses.getFirst().node()).isSameAs(m);
        assertThat(c.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(c.caughtException()).isInstanceOf(CaughtExceptionProjection.class);
    }
}