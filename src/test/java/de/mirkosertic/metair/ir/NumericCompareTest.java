package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class NumericCompareTest {

    @Test
    public void testUsage() {
        final PrimitiveFloat a = new PrimitiveFloat(10);
        final PrimitiveFloat b = new PrimitiveFloat(20);
        final NumericCompare compare = new NumericCompare(NumericCompare.Mode.NAN_IS_MINUS_1, ConstantDescs.CD_float, a, b);

        assertThat(compare.debugDescription()).isEqualTo("NumericCompare : NAN_IS_MINUS_1 for float");

        assertThat(compare.type).isEqualTo(ConstantDescs.CD_int);
        assertThat(compare.compareType).isEqualTo(ConstantDescs.CD_float);
        assertThat(compare.mode).isEqualTo(NumericCompare.Mode.NAN_IS_MINUS_1);
        assertThat(a).isInstanceOf(Value.class);
        assertThat(a.usedBy).containsExactly(compare);
        assertThat(b.usedBy).containsExactly(compare);
        assertThat(compare.uses.size()).isEqualTo(2);
        assertThat(compare.uses.get(0).node()).isSameAs(a);
        assertThat(compare.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(compare.uses.get(1).node()).isSameAs(b);
        assertThat(compare.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
        assertThat(compare.isConstant()).isFalse();
    }

}