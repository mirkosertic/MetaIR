package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveLong extends PrimitiveValue {

    public final long value;

    PrimitiveLong(final long value) {
        super(ConstantDescs.CD_long);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "long " + value;
    }
}
