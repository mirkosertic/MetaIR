package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class NumericCompare extends Value {

    public enum Mode {
        NONFLOATINGPOINT, NAN_IS_1, NAN_IS_MINUS_1
    }

    public final Mode mode;

    NumericCompare(final Mode mode, final Value a, final Value b) {
        super(ConstantDescs.CD_int);

        this.mode = mode;

        use(a, new ArgumentUse(0));
        use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "NumericCompare : " + mode;
    }
}
