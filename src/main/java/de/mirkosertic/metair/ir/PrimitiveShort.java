package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveShort extends PrimitiveValue {

    public final short value;

    PrimitiveShort(final short value) {
        super(ConstantDescs.CD_short);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "short " + value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
