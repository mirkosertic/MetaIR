package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtractMethodArgProjectionTest {

    @Test
    public void testUsage() {
        final Method m = new Method();
        final ExtractMethodArgProjection a = new ExtractMethodArgProjection(ConstantDescs.CD_String, m, 1);

        assertThat(a.type).isEqualTo(ConstantDescs.CD_String);
        assertThat(a.index()).isEqualTo(1);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).isEmpty();
        assertThat(a.uses).hasSize(1);
        assertThat(a.uses.getFirst().node()).isSameAs(m);
        assertThat(a.uses.getFirst().use()).isEqualTo(new ArgumentUse(1));
        assertThat(a.debugDescription()).isEqualTo("arg1 : String");
        assertThat(a.name()).isEqualTo("arg1");
    }
}