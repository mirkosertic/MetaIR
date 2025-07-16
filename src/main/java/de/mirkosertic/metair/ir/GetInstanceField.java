package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

public class GetInstanceField extends Value {

    public final FieldInsnNode node;

    GetInstanceField(final FieldInsnNode node, final Value source) {
        super(Type.getType(node.desc));
        this.node = node;

        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetField : " + node.name + " : " + type;
    }
}
