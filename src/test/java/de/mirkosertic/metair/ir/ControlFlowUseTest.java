package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlFlowUseTest {

    @Test
    public void testUsage() {
        final ControlFlowUse controlFlowUse = new ControlFlowUse(FlowType.FORWARD);
        assertThat(controlFlowUse.type).isEqualTo(FlowType.FORWARD);
    }
}