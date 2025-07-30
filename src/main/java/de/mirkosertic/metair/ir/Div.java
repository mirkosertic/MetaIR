package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Div extends Value {

    Div(final ClassDesc type, final Value arg1, final Value arg2) {
        super(type);
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "Div : " + DebugUtils.toString(type);
    }
}
