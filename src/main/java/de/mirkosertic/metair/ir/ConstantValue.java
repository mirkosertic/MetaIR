package de.mirkosertic.metair.ir;

public abstract class ConstantValue extends Value {

    ConstantValue(final IRType<?> type) {
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
