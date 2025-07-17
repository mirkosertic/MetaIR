package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveBoolean extends Value {

    public final boolean value;

    PrimitiveBoolean(final boolean value) {
        super(ConstantDescs.CD_boolean);
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
