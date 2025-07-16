package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class PrimitiveByte extends Value {

    public final int value;

    PrimitiveByte(final int value) {
        super(Type.BYTE_TYPE);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "byte " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
