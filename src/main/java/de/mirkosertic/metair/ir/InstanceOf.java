package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class InstanceOf extends Value {

    public final Value arg0;
    public final Value arg1;

    InstanceOf(final Value arg1, final Value arg2) {
        super(ConstantDescs.CD_int);
        this.arg0 = use(arg1, new ArgumentUse(0));
        this.arg1 = use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
