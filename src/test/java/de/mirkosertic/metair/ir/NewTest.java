package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class NewTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference ri = new RuntimeclassReference(ConstantDescs.CD_String);
        final New a = new New(ri);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node).isSameAs(ri);
        assertThat(a.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(a.isConstant()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("New");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}