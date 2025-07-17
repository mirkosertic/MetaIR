package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ThisRef extends Value {

    ThisRef(final ClassDesc type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "this : " + DebugUtils.toString(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
