package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ArrayLength extends Value {

    ArrayLength(final Value array) {
        super(ConstantDescs.CD_int);
        use(array, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
