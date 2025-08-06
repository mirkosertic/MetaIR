package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ExtractMethodArgProjection extends Value implements Projection {

    private final int index;

    ExtractMethodArgProjection(final ClassDesc type, final int index) {
        super(type);
        this.index = index;
    }

    public int index() {
        return index;
    }

    @Override
    public String name() {
        return "arg" + index;
    }

    @Override
    public String debugDescription() {
        return "arg" + index + " : " + TypeUtils.toString(type);
    }
}
