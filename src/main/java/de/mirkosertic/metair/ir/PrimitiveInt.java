package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveInt extends Value {

    public final int value;

    PrimitiveInt(final int value) {
        super(ConstantDescs.CD_int);
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
