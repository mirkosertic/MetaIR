package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {

    @Test
    public void testUsage() {
        final Result a = new Result(ConstantDescs.CD_int);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isFalse();
        assertThat(a.debugDescription()).isEqualTo("Result : int");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}