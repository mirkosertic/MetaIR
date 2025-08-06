package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ReturnValueTest {

    @Test
    public void testUsage() {
        final PrimitiveInt iv = new PrimitiveInt(10);
        final ReturnValue ret = new ReturnValue(ConstantDescs.CD_int, iv);

        assertThat(ret.debugDescription()).isEqualTo("ReturnValue : int");

        assertThat(ret.uses.size()).isEqualTo(1);
        assertThat(ret.uses.getFirst().node()).isSameAs(iv);
        assertThat(ret.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));
        assertThat(ret.usedBy).isEmpty();
        assertThat(ret.isConstant()).isTrue();
    }
}