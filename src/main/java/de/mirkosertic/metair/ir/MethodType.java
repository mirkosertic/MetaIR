package de.mirkosertic.metair.ir;

public class MethodType extends ConstantValue {

    MethodType(final IRType type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "MethodType : " + TypeUtils.toString(type);
    }
}
