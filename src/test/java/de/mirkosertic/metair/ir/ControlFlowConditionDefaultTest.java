package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ControlFlowConditionDefaultTest {

    @Test
    public void testUsage() {
        assertThat(ControlFlowConditionDefault.INSTANCE.debugDescription()).isEqualTo("default");
    }
}