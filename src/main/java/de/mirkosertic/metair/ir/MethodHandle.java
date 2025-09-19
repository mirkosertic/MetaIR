package de.mirkosertic.metair.ir;

public class MethodHandle extends ConstantValue {

    MethodHandle(final IRType.MethodHandle type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "MethodHandle : " + TypeUtils.toString(type);
    }
}
