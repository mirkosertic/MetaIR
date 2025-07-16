package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

public class GetStaticField extends Value {

    public final FieldInsnNode node;

    GetStaticField(final FieldInsnNode node, final RuntimeclassReference source) {
        super(Type.getType(node.desc));
        this.node = node;
        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetStaticField : " + node.name + " : " + type;
    }
}
