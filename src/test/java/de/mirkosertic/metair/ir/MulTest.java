package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class MulTest {

    @Test
    public void testUsage() {
        final PrimitiveInt a = new PrimitiveInt(10);
        final PrimitiveInt b = new PrimitiveInt(20);
        final Mul mul = new Mul(ConstantDescs.CD_int, a, b);

        assertThat(mul.debugDescription()).isEqualTo("Mul : int");

        assertThat(mul.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(mul);
        assertThat(b.usedBy).containsExactly(mul);
        assertThat(mul.uses.size()).isEqualTo(2);
        assertThat(mul.uses.get(0).node).isSameAs(a);
        assertThat(mul.uses.get(0).use).isEqualTo(new ArgumentUse(0));
        assertThat(mul.uses.get(1).node).isSameAs(b);
        assertThat(mul.uses.get(1).use).isEqualTo(new ArgumentUse(1));
        assertThat(mul.isConstant()).isFalse();

        assertThat(mul.peepholeOptimization()).isEmpty();
    }
}