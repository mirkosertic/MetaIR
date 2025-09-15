package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class GotoTest {

    @Test
    public void testUsage() {
        final Goto g = new Goto();

        assertThat(g.usedBy).isEmpty();
        assertThat(g.usedBy).isEmpty();
        assertThat(g.uses).isEmpty();

        assertThat(g.debugDescription()).isEqualTo("Goto");

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(g::getJumpTarget).withMessage("Cannot find the jump target");

        final Node next = new LabelNode("label");
        g.controlFlowsTo(next, FlowType.FORWARD);

        assertThat(g.getJumpTarget()).isSameAs(next);
    }
}