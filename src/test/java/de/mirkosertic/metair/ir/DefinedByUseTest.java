package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.*;

public class DefinedByUseTest {

    @Test
    public void testUsage() {
        final Label label = new Label("label");
        final PHI p = label.definePHI(Type.INT_TYPE);

        assertThat(p.uses).hasSize(1);
        assertThat(p.uses.getFirst().node).isSameAs(label);
        assertThat(p.uses.getFirst().use).isInstanceOf(DefinedByUse.class);

        assertThat(p.peepholeOptimization()).isEmpty();
    }
}