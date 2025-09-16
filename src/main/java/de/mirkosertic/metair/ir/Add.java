package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Add extends Value {

    public Add(final ClassDesc type, final Value arg1, final Value arg2) {
        super(type);

        if (!arg1.type.equals(type)) {
            illegalArgument("Cannot add non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg1.type) + " for arg1");
        }
        if (!arg2.type.equals(type)) {
            illegalArgument("Cannot add non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg2.type) + " for arg2");
        }

        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "Add : " + TypeUtils.toString(type);
    }
}
