package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class Null extends Value {

    Null() {
        super(ConstantDescs.CD_Object);
    }

    @Override
    public String debugDescription() {
        return "null";
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
