package de.mirkosertic.metair.ir;

import java.lang.classfile.instruction.FieldInstruction;

public class GetStaticField extends Value {

    public final FieldInstruction node;

    GetStaticField(final FieldInstruction node, final RuntimeclassReference source) {
        super(node.typeSymbol());
        this.node = node;
        use(source, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "GetStaticField : " + node.name() + " : " + DebugUtils.toString(type);
    }
}
