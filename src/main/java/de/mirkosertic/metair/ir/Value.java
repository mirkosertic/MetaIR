package de.mirkosertic.metair.ir;

public abstract class Value extends Node {

    public final IRType type;

    protected Value(final IRType type) {
        this.type = type;
    }

    public boolean isArray() {
        return type instanceof final IRType.MetaClass cls && cls.isArray();
    }

    public boolean isPrimitive() {
        return type instanceof final IRType.MetaClass cls && cls.isPrimitive();
    }
}
