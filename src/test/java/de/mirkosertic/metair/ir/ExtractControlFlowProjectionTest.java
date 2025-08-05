package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractControlFlowProjectionTest {

    @Test
    public void testUsage() {
        final ExtractControlFlowProjection projection = new ExtractControlFlowProjection("ctrl");

        assertThat(projection.debugDescription()).isEqualTo("ctrl");
        assertThat(projection.name()).isEqualTo("ctrl");

        assertThat(projection).isInstanceOf(Projection.class);
        assertThat(projection.uses).isEmpty();
        assertThat(projection.isConstant()).isFalse();

        assertThat(projection.peepholeOptimization()).isEmpty();
    }
}