package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Result extends Value {

    Result(final ClassDesc type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "Result : " + DebugUtils.toString(type);
    }
}
