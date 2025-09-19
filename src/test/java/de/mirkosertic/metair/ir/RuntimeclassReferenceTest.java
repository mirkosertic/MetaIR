
package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeclassReferenceTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference a = new RuntimeclassReference(IRType.CD_String);

        assertThat(a.type).isEqualTo(IRType.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("Class String");

        assertThat(MetaIRTestHelper.toDebugExpression(a)).isEqualTo("String.class");
    }
}