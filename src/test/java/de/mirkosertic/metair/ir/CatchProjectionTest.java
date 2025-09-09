package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CatchProjectionTest {

    @Test
    public void testUsage_Exception() {
        final CatchProjection projection = new CatchProjection(1, List.of(ClassDesc.of(IllegalArgumentException.class.getName())));

        assertThat(projection.debugDescription()).isEqualTo("catch : 1 : IllegalArgumentException");

        assertThat(projection.name()).isEqualTo("catch : 1 : IllegalArgumentException");
        assertThat(projection.index).isEqualTo(1);
        assertThat(projection.exceptionTypes).isEqualTo(List.of(ClassDesc.of(IllegalArgumentException.class.getName())));
        assertThat(projection.uses).isEmpty();
        assertThat(projection.usedBy).isEmpty();
        assertThat(projection.isConstant()).isFalse();
    }
}