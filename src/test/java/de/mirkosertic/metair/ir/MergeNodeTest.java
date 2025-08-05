package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeNodeTest {

    @Test
    public void testUsage() {
        final MergeNode node = new MergeNode("label");

        assertThat(node.debugDescription()).isEqualTo("Merge : label");

        assertThat(node.uses).isEmpty();
        assertThat(node.usedBy).isEmpty();
        assertThat(node).isInstanceOf(MultiInputNode.class);
        assertThat(node.isConstant()).isFalse();

        assertThat(node.peepholeOptimization()).isEmpty();
    }
}