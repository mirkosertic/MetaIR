package de.mirkosertic.metair.ir;

import java.lang.constant.MethodHandleDesc;

public class MethodHandle extends ConstantValue {

    MethodHandle(final MethodHandleDesc type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "MethodHandle : " + TypeUtils.toString(type);
    }
}
