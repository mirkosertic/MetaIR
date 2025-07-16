package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class PrimitiveInt extends Value {

    public final int value;

    PrimitiveInt(final int value) {
        super(Type.INT_TYPE);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "int " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
