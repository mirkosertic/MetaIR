package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class RuntimeclassReference extends Value {

    RuntimeclassReference(final Type type) {
        super(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Class " + type;
    }
}
