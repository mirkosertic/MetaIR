package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class StringConstant extends Value {

    public final String value;

    StringConstant(final String value) {
        super(ConstantDescs.CD_String);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "String : " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
