package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class PutStaticTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference target = new RuntimeclassReference(IRType.CD_String);
        final PrimitiveInt value = new PrimitiveInt(10);
        final PutStatic put = new PutStatic(target, "fieldname", IRType.CD_int, value);

        assertThat(target.usedBy).containsExactly(put);

        assertThat(put.uses).hasSize(2);
        assertThat(put.uses.get(0).node()).isSameAs(target);
        assertThat(put.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
        assertThat(put.uses.get(1).node()).isSameAs(value);
        assertThat(put.uses.get(1).use()).isEqualTo(new ArgumentUse(1));

        assertThat(put.debugDescription()).isEqualTo("PutStaticField : fieldname : int");
        assertThat(put.fieldName).isEqualTo("fieldname");
        assertThat(put.isConstant()).isFalse();
    }


    @Test
    public void testUsage_put_int_to_boolean() {
        final RuntimeclassReference target = new RuntimeclassReference(IRType.CD_String);
        final PrimitiveInt value = new PrimitiveInt(10);
        final PutStatic put = new PutStatic(target, "fieldname", IRType.CD_boolean, value);
    }

    @Test
    public void fail_wrongtype_field_is_primitive() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new PutStatic(new RuntimeclassReference(IRType.CD_String), "fieldname", IRType.CD_int, new StringConstant("hello"));
            fail("Exception expected");
        }).withMessage("Cannot put value of type String in field fieldname of type int");
    }

    @Test
    public void fail_wrongtype_field_is_primitive_boolean() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new PutStatic(new RuntimeclassReference(IRType.CD_String), "fieldname", IRType.CD_boolean, new StringConstant("hello"));
            fail("Exception expected");
        }).withMessage("Cannot put value of type String in field fieldname of type boolean");
    }
}