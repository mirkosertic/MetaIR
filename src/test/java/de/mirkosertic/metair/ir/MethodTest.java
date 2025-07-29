package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodTest {

    @Test
    public void testSameStringConstants() {
        final Method method = new Method(null);

        final StringConstant r1 = method.defineStringConstant("a");
        final StringConstant r2 = method.defineStringConstant("a");

        assertThat(r1).isSameAs(r2);
        assertThat(r1.usedBy).isEmpty();

        assertThat(r1).isSameAs(r2);
    }

    @Test
    public void testSameClassRefs() {
        final Method method = new Method(null);

        final RuntimeclassReference r1 = method.defineRuntimeclassReference(ConstantDescs.CD_String);
        final RuntimeclassReference r2 = method.defineRuntimeclassReference(ConstantDescs.CD_String);

        assertThat(r1).isSameAs(r2);
        assertThat(r1.usedBy).isEmpty();

        assertThat(r1).isSameAs(r2);
    }

}