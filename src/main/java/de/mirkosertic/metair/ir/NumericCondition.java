package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class NumericCondition extends Value {

    public enum Operation {
        EQ, NE, LT, GE, GT, LE
    }

    public final Operation operation;
    public final Value arg0;
    public final Value arg1;

    NumericCondition(final Operation operation, final Value a, final Value b) {
        super(ConstantDescs.CD_int);

        this.operation = operation;

        this.arg0 = use(a, new ArgumentUse(0));
        this.arg1 = use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "NumericCondition : " + operation;
    }
}
