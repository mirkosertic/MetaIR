package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Sub extends Value {

    public final Value arg1;
    public final Value arg2;

    Sub(final ClassDesc type, final Value arg1, final Value arg2) {
        super(type);

        if (!arg1.type.equals(type)) {
            illegalArgument("Cannot subtract non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg1.type) + " for arg1");
        }
        if (!arg2.type.equals(type)) {
            illegalArgument("Cannot subtract non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg2.type) + " for arg2");
        }

        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));

        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String debugDescription() {
        return "Sub : " + TypeUtils.toString(type);
    }
}
