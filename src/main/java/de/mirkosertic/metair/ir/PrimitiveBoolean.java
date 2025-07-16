package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class PrimitiveBoolean extends Value {

    public final boolean value;

    PrimitiveBoolean(final boolean value) {
        super(Type.BOOLEAN_TYPE);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "boolean " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
