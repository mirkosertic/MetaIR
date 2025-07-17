package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodArgumentTest {

    @Test
    public void testUsage() {
        final MethodArgument a = new MethodArgument(ConstantDescs.CD_String, 1);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a.index).isEqualTo(1);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("arg 1 : String");

        assertThat(a.peepholeOptimization()).isEmpty();
    }
}