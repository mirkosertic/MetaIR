package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class MethodArgument extends Value {

    public final int index;

    MethodArgument(final Type type, final int index) {
        super(type);
        this.index = index;
    }

    @Override
    public String debugDescription() {
        return "arg " + index + " : " + type;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
