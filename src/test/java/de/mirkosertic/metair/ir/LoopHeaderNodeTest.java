package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoopHeaderNodeTest {

    @Test
    public void testUsage() {
        final LoopHeaderNode node = new LoopHeaderNode("label");

        assertThat(node.debugDescription()).isEqualTo("LoopHeader : label");

        assertThat(node.uses).isEmpty();
        assertThat(node.usedBy).isEmpty();
        assertThat(node).isInstanceOf(Node.class);
        assertThat(node.isConstant()).isFalse();
    }
}