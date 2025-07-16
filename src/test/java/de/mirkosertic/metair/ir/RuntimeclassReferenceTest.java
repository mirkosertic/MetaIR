
package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeclassReferenceTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference a = new RuntimeclassReference(Type.getType(String.class));

        assertThat(a.type.getClassName()).isEqualTo(String.class.getName());
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("Class Ljava/lang/String;");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}