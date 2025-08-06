package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class AddTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Add add = new Add(ConstantDescs.CD_int, a, b);

        assertThat(add.debugDescription()).isEqualTo("Add : int");

        assertThat(add.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(add);
        assertThat(b.usedBy).containsExactly(add);
        assertThat(add.uses.size()).isEqualTo(2);
        assertThat(add.uses.get(0).node()).isSameAs(a);
        assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(add.uses.get(1).node()).isSameAs(b);
        assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(add.isConstant()).isFalse();
    }
}