package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CatchProjectionTest {

    @Test
    public void testUsage_Exception() {
        final CatchProjection projection = new CatchProjection(1, List.of(IRType.MetaClass.of(IllegalArgumentException.class)));

        assertThat(projection.debugDescription()).isEqualTo("catch : 1 : IllegalArgumentException");

        assertThat(projection.name()).isEqualTo("catch : 1 : IllegalArgumentException");
        assertThat(projection.index).isEqualTo(1);
        assertThat(projection.exceptionTypes).isEqualTo(List.of(IRType.MetaClass.of(IllegalArgumentException.class)));
        assertThat(projection.uses).isEmpty();
        assertThat(projection.usedBy).isEmpty();
        assertThat(projection.isConstant()).isFalse();
    }
}