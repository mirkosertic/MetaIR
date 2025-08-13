package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class PrimitiveFloat extends PrimitiveValue {

    public final float value;

    PrimitiveFloat(final float value) {
        super(ConstantDescs.CD_float);
        this.value = value;
    }

    @Override
    public String debugDescription() {
        return "float " + value;
    }
}
