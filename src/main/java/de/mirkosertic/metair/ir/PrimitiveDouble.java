package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveDouble extends Value {

    public final double value;

    PrimitiveDouble(final double value) {
        super(ConstantDescs.CD_double);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "double " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
