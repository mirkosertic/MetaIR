package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractThisRefProjectionTest {

    @Test
    public void testUsage() {
        final ExtractThisRefProjection a = new ExtractThisRefProjection(ConstantDescs.CD_String);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.debugDescription()).isEqualTo("this : String");
        assertThat(a.name()).isEqualTo("this");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}