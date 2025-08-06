package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

public class GetFieldTest {

    @Test
    public void testUsage() {
        final FieldInstruction node = FieldInstruction.of(Opcode.GETFIELD, ConstantPoolBuilder.of().fieldRefEntry(ConstantDescs.CD_String, "field", ConstantDescs.CD_int));
        final Value v = new StringConstant("Hello");
        final GetField get = new GetField(node, v);

        assertThat(v.usedBy).containsExactly(get);

        assertThat(get.uses).hasSize(1);
        assertThat(get.uses.getFirst().node()).isSameAs(v);
        assertThat(get.uses.getFirst().use()).isEqualTo(new ArgumentUse(0));

        assertThat(get.debugDescription()).isEqualTo("GetField : field : int");
        assertThat(get.isConstant()).isFalse();
    }
}