package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class InstanceOf extends Value {

    InstanceOf(final Value arg1, final Value arg2) {
        super(ConstantDescs.CD_int);
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
