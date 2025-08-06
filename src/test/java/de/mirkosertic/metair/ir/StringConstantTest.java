package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class StringConstantTest {

    @Test
    public void testUsage() {
        final StringConstant a = new StringConstant("hello");

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).isEmpty();
        assertThat(a.isConstant()).isTrue();
        assertThat(a.debugDescription()).isEqualTo("String : hello");
        assertThat(a.value).isEqualTo("hello");
    }
}