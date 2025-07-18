package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassInitializationTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference cr = new RuntimeclassReference(ConstantDescs.CD_String);
        final ClassInitialization ci = new ClassInitialization(cr);

        assertThat(ci.debugDescription()).isEqualTo("ClassInit");

        assertThat(ci.uses.size()).isEqualTo(1);
        assertThat(ci.uses.getFirst().node).isSameAs(cr);
        assertThat(ci.uses.getFirst().use).isEqualTo(new ArgumentUse(0));
        assertThat(ci.usedBy).isEmpty();
        assertThat(ci.isConstant()).isFalse();

        assertThat(ci.peepholeOptimization()).isEmpty();
    }
}