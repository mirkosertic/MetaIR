package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class NewArray extends Value {

    NewArray(final ClassDesc type, final Value length) {
        super(type);
        use(length, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "NewArray : " + DebugUtils.toString(type);
    }
}
