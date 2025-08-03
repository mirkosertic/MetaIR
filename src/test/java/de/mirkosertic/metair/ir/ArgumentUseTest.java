package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgumentUseTest {

    @Test
    public void testUsage() {
        final ArgumentUse argumentUse = new ArgumentUse(10);
        assertThat(argumentUse).isInstanceOf(DataFlowUse.class);
        assertThat(argumentUse.index).isEqualTo(10);
        assertThat(argumentUse).isEqualTo(new ArgumentUse(10));
        assertThat(argumentUse.hashCode()).isEqualTo(10);
    }
}