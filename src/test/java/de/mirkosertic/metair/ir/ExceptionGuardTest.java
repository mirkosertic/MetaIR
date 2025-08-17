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
        assertThat(guard.exitNode()).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> ((ExtractControlFlowProjection) t).name().equals("exit"));
        assertThat(guard.getNamedNode("default")).isInstanceOf(ExtractControlFlowProjection.class).matches(t -> ((ExtractControlFlowProjection) t).name().equals("default"));
        assertThat(guard.getNamedNode("catch:0:IllegalArgumentException")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionType.equals(ClassDesc.of(IllegalArgumentException.class.getName())));
        assertThat(guard.getNamedNode("catch:1:any")).isInstanceOf(Catch.class).matches(t -> ((Catch) t).exceptionType.equals(ClassDesc.of(Throwable.class.getName())));
    }
}