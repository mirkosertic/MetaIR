package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDesc;

public abstract class ConstantValue extends Value {

    ConstantValue(final ConstantDesc type) {
        super(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }
}
