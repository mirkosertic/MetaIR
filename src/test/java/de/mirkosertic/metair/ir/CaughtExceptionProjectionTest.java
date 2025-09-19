package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaughtExceptionProjectionTest {

    @Test
    public void testUsage() {
        final Catch source = new Catch(List.of(IRType.MetaClass.of(Exception.class)), new Method());
        final CaughtExceptionProjection projection = new CaughtExceptionProjection(source);
        assertThat(projection.debugDescription()).isEqualTo("exception : Exception");
        assertThat(projection.name()).isEqualTo("exception");

        assertThat(projection.type).isEqualTo(IRType.MetaClass.of(Exception.class));
        assertThat(projection).isInstanceOf(Value.class);
        assertThat(projection).isInstanceOf(Projection.class);
        assertThat(projection.uses.size()).isEqualTo(1);
        assertThat(projection.uses.getFirst().node()).isSameAs(source);
        assertThat(projection.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(projection.isConstant()).isTrue();
    }
}