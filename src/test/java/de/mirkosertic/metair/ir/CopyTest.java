package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class CopyTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PHI b = new PHI(ConstantDescs.CD_int);
        final Copy copy = new Copy(a, b);

        assertThat(a.usedBy).containsExactly(copy);
        assertThat(b.usedBy).isEmpty();
        assertThat(copy.uses.size()).isEqualTo(1);
        assertThat(copy.uses.getFirst().node).isSameAs(a);
        assertThat(copy.uses.getFirst().use).isInstanceOf(DataFlowUse.class);

        assertThat(b.isConstant()).isTrue();

        assertThat(copy.peepholeOptimization()).isEmpty();

        assertThat(copy.debugDescription()).isEqualTo("Copy");
    }
}