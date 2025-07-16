package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodArgumentTest {

    @Test
    public void testUsage() {
        final MethodArgument a = new MethodArgument(Type.getType(String.class), 1);

        assertThat(a.type.getClassName()).isEqualTo(String.class.getName());
        assertThat(a.index).isEqualTo(1);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("arg 1 : Ljava/lang/String;");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}