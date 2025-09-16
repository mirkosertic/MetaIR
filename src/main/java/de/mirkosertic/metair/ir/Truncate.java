package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Truncate extends Value {

    Truncate(final ClassDesc targetType, final Value value) {
        super(targetType);

        use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Truncate : " + TypeUtils.toString(type);
    }
}
