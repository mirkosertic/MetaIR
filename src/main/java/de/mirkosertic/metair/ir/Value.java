package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public abstract class Value extends Node {

    public final ClassDesc type;

    protected Value(final ClassDesc type) {
        this.type = type;
    }
}
