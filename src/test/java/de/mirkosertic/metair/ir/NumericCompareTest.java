package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class NumericCompareTest {

    @Test
    public void testUsage() {
        final PrimitiveFloat a = new PrimitiveFloat(10);
        final PrimitiveFloat b = new PrimitiveFloat(20);
        final NumericCompare compare = new NumericCompare(NumericCompare.Mode.NAN_IS_MINUS_1, IRType.CD_float, a, b);

        assertThat(compare.debugDescription()).isEqualTo("NumericCompare : NAN_IS_MINUS_1 for float");

        assertThat(compare.sideeffectFree()).isTrue();

        assertThat(compare.type).isEqualTo(IRType.CD_int);
        assertThat(compare.compareType).isEqualTo(IRType.CD_float);
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

    @Test
    public void fail_wrongtype_arg1() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new NumericCompare(NumericCompare.Mode.NAN_IS_1, IRType.CD_int, new PrimitiveLong(10L), new PrimitiveInt(10));
            fail("Exception expected");
        }).withMessage("Cannot compare non int value long for arg1");
    }

    @Test
    public void fcmpg_fail_value2() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new NumericCompare(NumericCompare.Mode.NAN_IS_1, IRType.CD_int, new PrimitiveInt(10), new PrimitiveLong(10L));
            fail("Exception expected");
        }).withMessage("Cannot compare non int value long for arg2");
    }
}