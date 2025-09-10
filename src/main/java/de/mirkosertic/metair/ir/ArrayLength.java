package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ArrayLength extends Value {

    public final Value arg0;

    ArrayLength(final Value array) {
        super(ConstantDescs.CD_int);

        if (!array.isArray()) {
            illegalArgument("Cannot get array length of non array of type " + TypeUtils.toString(array.type));
        }

        this.arg0 = use(array, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
