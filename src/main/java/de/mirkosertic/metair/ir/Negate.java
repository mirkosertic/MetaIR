package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Negate extends Value {

    Negate(final ClassDesc type, final Value arg1) {
        super(type);
        use(arg1, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Negate : " + TypeUtils.toString(type);
    }
}
