package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionGuardTest {

    @Test
    public void testUsage() {
        final ExceptionGuard guard = new ExceptionGuard("Guard0", List.of(new ExceptionGuard.Catches(0, List.of(ClassDesc.of(IllegalArgumentException.class.getName()))), new ExceptionGuard.Catches(1, Collections.emptyList())));

        assertThat(guard.startLabel).isEqualTo("Guard0");
        assertThat(guard.debugDescription()).isEqualTo("ExceptionGuard");
        assertThat(guard.exitNode()).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> t.name().equals("exit"));
        assertThat(guard.getNamedNode("default")).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> ((ExtractControlFlowProjection) t).name().equals("default"));
        assertThat(guard.getNamedNode("catch : 0 : IllegalArgumentException")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionTypes.equals(List.of(ClassDesc.of(IllegalArgumentException.class.getName()))));
        assertThat(guard.getNamedNode("catch : 1 : any")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionTypes.isEmpty());

        assertThat(guard.guardedBlock().name()).isEqualTo("default");
        assertThat(guard.catchProjection(0).exceptionTypes).isEqualTo(List.of(ClassDesc.of(IllegalArgumentException.class.getName())));
        assertThat(guard.catchProjection(1).exceptionTypes).isEmpty();
    }
}