package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlFlowUseTest {

    @Test
    public void testUsage() {
        final ControlFlowUse controlFlowUse = new ControlFlowUse(ControlType.FORWARD, ControlFlowConditionDefault.INSTANCE);
        assertThat(controlFlowUse.type).isEqualTo(ControlType.FORWARD);
        assertThat(controlFlowUse.condition).isEqualTo(ControlFlowConditionDefault.INSTANCE);
    }
}