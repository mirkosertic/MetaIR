package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ExtractThisRefProjection extends Value implements Projection {

    ExtractThisRefProjection(final ClassDesc type) {
        super(type);
    }

    @Override
    public String name() {
        return "this";
    }

    @Override
    public String debugDescription() {
        return "this : " + TypeUtils.toString(type);
    }
}
