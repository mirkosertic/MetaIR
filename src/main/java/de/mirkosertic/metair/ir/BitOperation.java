package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class BitOperation extends Value {

    public enum Operation {
        AND, OR, XOR, SHL, SHR, USHR
    }

    public final Operation operation;
    public final Value arg0;
    public final Value arg1;

    BitOperation(final ClassDesc type, final Operation operation, final Value arg1, final Value arg2) {
        super(type);

        if (!arg1.type.equals(type)) {
            illegalArgument("Cannot use non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg1.type) + " for bit operation " + operation + " on arg1");
        }
        if (!arg2.type.equals(type)) {
            illegalArgument("Cannot use non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(arg2.type) + " for bit operation " + operation + " on arg2");
        }

        this.operation = operation;
        this.arg0 = use(arg1, new ArgumentUse(0));
        this.arg1 = use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "BitOperation : " + operation + "(" + TypeUtils.toString(type) + ")";
    }
}
