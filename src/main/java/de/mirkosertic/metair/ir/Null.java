package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class Null extends ConstantValue {

    Null() {
        super(ConstantDescs.CD_Object);
    }

    @Override
    public String debugDescription() {
        return "null";
    }
}
