package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LabelNodeTest {

    @Test
    public void testUsage() {
        final LabelNode a = new LabelNode("label");
        assertThat(a.label).isEqualTo("label");
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();

        assertThat(a.debugDescription()).isEqualTo("LabelNode");
    }

    @Test
    public void testPHI() {
        final LabelNode a = new LabelNode("label");
        assertThat(a.usedBy).isEmpty();

        final PHI p = a.definePHI(IRType.CD_int);
        assertThat(p.type).isEqualTo(IRType.CD_int);
        assertThat(p.uses.getFirst().node()).isSameAs(a);
        assertThat(p.uses.getFirst().use()).isSameAs(DefinedByUse.INSTANCE);
        assertThat(p.usedBy).isEmpty();
        assertThat(p.isConstant()).isFalse();
        assertThat(p.debugDescription()).isEqualTo("Φ int");

        assertThat(a.usedBy).containsExactly(p);
    }

}