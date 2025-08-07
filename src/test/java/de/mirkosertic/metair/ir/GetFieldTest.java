package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class GetFieldTest {

    @Test
    public void testUsage() {
        final Value v = new StringConstant("Hello");
        final GetField get = new GetField(ConstantDescs.CD_String, ConstantDescs.CD_int, "field", v);

        assertThat(v.usedBy).containsExactly(get);

        assertThat(get.uses).hasSize(1);
        assertThat(get.uses.getFirst().node()).isSameAs(v);
        assertThat(get.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(get.debugDescription()).isEqualTo("GetField : field : int");
        assertThat(get.isConstant()).isFalse();
    }
}