package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionGuardTest {

    @Test
    public void testUsage() {
        final ExceptionGuard guard = new ExceptionGuard(List.of(new ExceptionGuard.Catches(Optional.of(ClassDesc.of(IllegalArgumentException.class.getName()))), new ExceptionGuard.Catches(Optional.empty())));

        assertThat(guard.debugDescription()).isEqualTo("ExceptionGuard");
        assertThat(guard.exitNode()).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> t.name().equals("exit"));
        assertThat(guard.getNamedNode("default")).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> ((ExtractControlFlowProjection) t).name().equals("default"));
        assertThat(guard.getNamedNode("catch:0:Ljava/lang/IllegalArgumentException;")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionType.equals(ClassDesc.of(IllegalArgumentException.class.getName())));
        assertThat(guard.getNamedNode("catch:1:any")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionType.equals(ClassDesc.of(Throwable.class.getName())));

        assertThat(guard.guardedBlock().name()).isEqualTo("default");
        assertThat(guard.catchProjection(0).exceptionType).isEqualTo(ClassDesc.of(IllegalArgumentException.class.getName()));
        assertThat(guard.catchProjection(1).exceptionType).isEqualTo(ClassDesc.of(Throwable.class.getName()));
    }
}