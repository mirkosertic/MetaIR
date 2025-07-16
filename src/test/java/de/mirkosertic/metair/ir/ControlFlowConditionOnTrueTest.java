package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ControlFlowConditionOnTrueTest {

    @Test
    public void testUsage() {
        assertThat(ControlFlowConditionOnTrue.INSTANCE.debugDescription()).isEqualTo("on true");
    }
}