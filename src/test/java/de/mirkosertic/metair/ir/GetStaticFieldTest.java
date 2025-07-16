package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

import static org.assertj.core.api.Assertions.assertThat;

public class GetStaticFieldTest {

    @Test
    public void testUsage() {
        final FieldInsnNode node = new FieldInsnNode(Opcodes.GETSTATIC, null, "field", Type.INT_TYPE.getDescriptor());
        final RuntimeclassReference v = new RuntimeclassReference(Type.getType(String.class));
        final GetStaticField get = new GetStaticField(node, v);

        assertThat(v.usedBy).containsExactly(get);

        assertThat(get.uses).hasSize(1);
        assertThat(get.uses.getFirst().node).isSameAs(v);
        assertThat(get.uses.getFirst().use).isEqualTo(new ArgumentUse(0));

        assertThat(get.peepholeOptimization()).isEmpty();
        assertThat(get.debugDescription()).isEqualTo("GetStaticField : field : I");
        assertThat(get.isConstant()).isFalse();
    }
}