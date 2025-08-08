package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class PutFieldTest {

    @Test
    public void testUsage() {
        final Value target = new StringConstant("str");
        final PrimitiveInt value = new PrimitiveInt(10);
        final PutField put = new PutField(ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", target, value);

        assertThat(target.usedBy).containsExactly(put);

        assertThat(put.uses).hasSize(2);
        assertThat(put.uses.get(0).node()).isSameAs(target);
        assertThat(put.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(put.uses.get(1).node()).isSameAs(value);
        assertThat(put.uses.get(1).use()).isEqualTo(new ArgumentUse(1));

        assertThat(put.debugDescription()).isEqualTo("PutField : fieldname : int");
        assertThat(put.fieldName).isEqualTo("fieldname");
        assertThat(put.isConstant()).isFalse();
    }

    @Test
    public void fail_wrongtype_target_is_primitive() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new PutField(ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", new PrimitiveInt(10), new StringConstant("hello"));
            fail("Exception expected");
        }).withMessage("Cannot put field fieldname on non object target int");
    }

    @Test
    public void fail_wrongtype_field_is_primitive() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new PutField(ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", new StringConstant("hello"), new StringConstant("hello"));
            fail("Exception expected");
        }).withMessage("Cannot put non primitive value String on field fieldname of type int");
    }
}