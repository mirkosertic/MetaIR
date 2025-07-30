package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveShortTest {

    @Test
    public void testUsage() {
        final PrimitiveShort a = new PrimitiveShort((short) 10);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_short);
        assertThat(a.value).isEqualTo((short) 10);
        assertThat(a).isInstanceOf(PrimitiveValue.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("short 10");

        assertThat(a.peepholeOptimization()).isEmpty();
    }

}