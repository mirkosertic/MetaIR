package de.mirkosertic.metair.ir;

import java.lang.constant.MethodTypeDesc;

public class MethodType extends ConstantValue {

    MethodType(final MethodTypeDesc type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "MethodType : " + TypeUtils.toString(type);
    }
}
