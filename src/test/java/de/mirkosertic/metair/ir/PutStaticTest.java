package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class PutStaticTest {

    @Test
    public void testUsage() {
        final RuntimeclassReference target = new RuntimeclassReference(ConstantDescs.CD_String);
        final PrimitiveInt value = new PrimitiveInt(10);
        final PutStatic put = new PutStatic(target, "fieldname", ConstantDescs.CD_int, value);

        assertThat(target.usedBy).containsExactly(put);

        assertThat(put.uses).hasSize(2);
        assertThat(put.uses.get(0).node).isSameAs(target);
        assertThat(put.uses.get(0).use).isEqualTo(new ArgumentUse(0));
        assertThat(put.uses.get(1).node).isSameAs(value);
        assertThat(put.uses.get(1).use).isEqualTo(new ArgumentUse(1));

        assertThat(put.peepholeOptimization()).isEmpty();
        assertThat(put.debugDescription()).isEqualTo("PutStaticField : fieldname : int");
        assertThat(put.fieldName).isEqualTo("fieldname");
        assertThat(put.isConstant()).isFalse();
    }
}