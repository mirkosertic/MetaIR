package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

public abstract class Value extends Node {

    public final ConstantDesc type;

    protected Value(final ConstantDesc type) {
        this.type = type;
    }

    public boolean isArray() {
        return type instanceof ClassDesc && ((ClassDesc) type).isArray();
    }

    public boolean isPrimitive() {
        return type instanceof ClassDesc && ((ClassDesc) type).isPrimitive();
    }
}
