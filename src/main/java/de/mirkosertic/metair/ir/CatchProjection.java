package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class CatchProjection extends Node implements Projection {

    public final int index;
    public final ClassDesc type;

    CatchProjection(final int index) {
        this.index = index;
        this.type = null;
    }

    CatchProjection(final int index, final ClassDesc type) {
        this.index = index;
        this.type = type;
    }

    @Override
    public String name() {
        if (type == null) {
            return "any";
        }
        return "catch : " + TypeUtils.toString(type);
    }

    @Override
    public String debugDescription() {
        return name();
    }
}
