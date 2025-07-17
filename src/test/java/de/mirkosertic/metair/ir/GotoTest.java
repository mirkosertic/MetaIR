package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GotoTest {

    @Test
    public void testUsage() {
        final Goto g = new Goto();

        assertThat(g.usedBy).isEmpty();
        assertThat(g.usedBy).isEmpty();
        assertThat(g.uses).isEmpty();

        assertThat(g.debugDescription()).isEqualTo("Goto");
    }
}