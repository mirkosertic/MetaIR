package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class MethodArgument extends Value {

    public final int index;

    MethodArgument(final ClassDesc type, final int index) {
        super(type);
        this.index = index;
    }

    @Override
    public String debugDescription() {
        return "arg " + index + " : " + DebugUtils.toString(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
