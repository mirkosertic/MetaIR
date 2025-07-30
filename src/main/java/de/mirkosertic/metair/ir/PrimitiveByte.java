package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveByte extends PrimitiveValue {

    public final int value;

    PrimitiveByte(final int value) {
        super(ConstantDescs.CD_byte);
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
