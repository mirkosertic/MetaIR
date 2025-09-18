package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Mul extends Value {

    Mul(final ClassDesc type, final Value arg1, final Value arg2) {
        super(type);

        if (!arg1.type.equals(type)) {
            illegalArgument("Cannot multiply non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg1.type) + " for arg1");
        }
        if (!arg2.type.equals(type)) {
            illegalArgument("Cannot multiply non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg2.type) + " for arg2");
        }

        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Mul : " + TypeUtils.toString(type);
    }
}
