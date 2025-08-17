package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;

import static org.assertj.core.api.Assertions.assertThat;

public class CatchProjectionTest {

    @Test
    public void testUsage_Exception() {
        final CatchProjection projection = new CatchProjection(1, ClassDesc.of(IllegalArgumentException.class.getName()));

        assertThat(projection.debugDescription()).isEqualTo("catch : IllegalArgumentException");

        assertThat(projection.name()).isEqualTo("catch : IllegalArgumentException");
        assertThat(projection.index).isEqualTo(1);
        assertThat(projection.type).isEqualTo(ClassDesc.of(IllegalArgumentException.class.getName()));
        assertThat(projection.uses).isEmpty();
        assertThat(projection.usedBy).isEmpty();
        assertThat(projection.isConstant()).isFalse();
    }

    @Test
    public void testUsage_Any() {
        final CatchProjection projection = new CatchProjection(1);

        assertThat(projection.debugDescription()).isEqualTo("any");

        assertThat(projection.name()).isEqualTo("any");
        assertThat(projection.index).isEqualTo(1);
        assertThat(projection.type).isNull();
        assertThat(projection.uses).isEmpty();
        assertThat(projection.usedBy).isEmpty();
        assertThat(projection.isConstant()).isFalse();
    }
}