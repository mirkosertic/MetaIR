package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public abstract class Value extends Node {

    public final Type type;

    protected Value(final Type type) {
        this.type = type;
    }

    @Override
    public String debugDescription() {
        return type.toString();
    }
}
