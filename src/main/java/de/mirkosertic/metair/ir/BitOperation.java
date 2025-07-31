package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class BitOperation extends Value {

    public enum Operation {
        AND, OR, XOR, SHL, SHR, USHR
    }

    public final Operation operation;

    BitOperation(final ClassDesc type, final Operation operation, final Value arg1, final Value arg2) {
        super(type);
        this.operation = operation;
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "BitOperation : " + operation + "(" + DebugUtils.toString(type) + ")";
    }
}
