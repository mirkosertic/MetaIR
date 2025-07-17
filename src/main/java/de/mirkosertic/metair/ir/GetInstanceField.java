package de.mirkosertic.metair.ir;

import java.lang.classfile.instruction.FieldInstruction;

public class GetInstanceField extends Value {

    public final FieldInstruction node;

    GetInstanceField(final FieldInstruction node, final Value source) {
        super(node.typeSymbol());
        this.node = node;

        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetField : " + node.name() + " : " + DebugUtils.toString(type);
    }
}
