package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Convert extends Value {

    public final ClassDesc from;
    public final Value arg0;

    Convert(final ClassDesc to, final Value arg1, final ClassDesc from) {
        super(to);

        if (!arg1.type.equals(from)) {
            illegalArgument("Expected a value of type " + TypeUtils.toString(from) + " but got " + TypeUtils.toString(arg1.type));
        }

        this.arg0 = use(arg1, new ArgumentUse(0));
        this.from = from;
    }

    @Override
    public String debugDescription() {
        return "Convert : " + TypeUtils.toString(from) + " to " + TypeUtils.toString(type);
    }
}
