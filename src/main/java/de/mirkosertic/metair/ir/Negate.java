package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Negate extends Value {

    public final Value arg0;

    Negate(final ClassDesc type, final Value arg1) {
        super(type);

        if (!arg1.type.equals(type)) {
            illegalArgument("Cannot negate non " + TypeUtils.toString(type) + " of type " + TypeUtils.toString(arg1.type));
        }

        this.arg0 = use(arg1, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "Negate : " + TypeUtils.toString(type);
    }
}
