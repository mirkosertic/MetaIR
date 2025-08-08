package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

public class NumericCompare extends Value {

    public enum Mode {
        NONFLOATINGPOINT, NAN_IS_1, NAN_IS_MINUS_1
    }

    public final ClassDesc compareType;
    public final Mode mode;

    NumericCompare(final Mode mode, final ClassDesc compareType, final Value arg1, final Value arg2) {
        super(ConstantDescs.CD_int);

        if (!arg1.type.equals(compareType)) {
            illegalArgument("Cannot compare non " + TypeUtils.toString(compareType) + " value " + TypeUtils.toString(arg1.type) + " for arg1");
        }

        if (!arg2.type.equals(compareType)) {
            illegalArgument("Cannot compare non " + TypeUtils.toString(compareType) + " value " + TypeUtils.toString(arg2.type) + " for arg2");
        }

        this.mode = mode;
        this.compareType = compareType;

        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "NumericCompare : " + mode + " for " + TypeUtils.toString(compareType);
    }
}
