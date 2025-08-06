package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveIntTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a.value).isEqualTo(10);
        assertThat(a).isInstanceOf(PrimitiveValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("int 10");
    }

}