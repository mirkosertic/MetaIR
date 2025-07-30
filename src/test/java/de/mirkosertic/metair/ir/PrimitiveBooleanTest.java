package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveBooleanTest {

    @Test
    public void testUsage() {
        final PrimitiveBoolean a = new PrimitiveBoolean(true);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_boolean);
        assertThat(a.value).isTrue();
        assertThat(a).isInstanceOf(PrimitiveValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("boolean true");

        assertThat(a.peepholeOptimization()).isEmpty();
    }

}