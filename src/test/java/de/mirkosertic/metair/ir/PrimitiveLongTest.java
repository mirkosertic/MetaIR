package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveLongTest {

    @Test
    public void testUsage() {
        final PrimitiveLong a = new PrimitiveLong(10L);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_long);
        assertThat(a.value).isEqualTo(10L);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("long 10");

        assertThat(a.peepholeOptimization()).isEmpty();
    }

}