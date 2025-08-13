package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public abstract class PrimitiveValue extends ConstantValue {

    public PrimitiveValue(final ClassDesc type) {
        super(type);
    }
}
