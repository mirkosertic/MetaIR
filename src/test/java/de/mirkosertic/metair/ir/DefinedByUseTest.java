package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class DefinedByUseTest {

    @Test
    public void testUsage() {
        final LabelNode label = new LabelNode("label");
        final PHI p = label.definePHI(ConstantDescs.CD_int);

        assertThat(p.type).isEqualTo(ConstantDescs.CD_int);

        assertThat(p.uses).hasSize(1);
        assertThat(p.uses.getFirst().node()).isSameAs(label);
        assertThat(p.uses.getFirst().use()).isInstanceOf(DefinedByUse.class);
    }
}