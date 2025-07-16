package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class ThisRef extends Value {

    ThisRef(final Type type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "this : " + type;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
