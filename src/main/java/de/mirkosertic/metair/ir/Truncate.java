package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Truncate extends Value {

    public final Value arg0;

    Truncate(final ClassDesc targetType, final Value value) {
        super(targetType);

        this.arg0 = use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Truncate : " + TypeUtils.toString(type);
    }
}
