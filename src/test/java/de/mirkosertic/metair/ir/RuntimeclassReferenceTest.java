
package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeclassReferenceTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference a = new RuntimeclassReference(ConstantDescs.CD_String);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("Class String");
    }
}