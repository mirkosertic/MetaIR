package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class NumericCondition extends Value {

    public enum Operation {
        EQ, NE, LT, GE, GT, LE
    }

    public final Operation operation;

    NumericCondition(final Operation operation, final Value a, final Value b) {
        super(ConstantDescs.CD_boolean);

        this.operation = operation;

        use(a, new ArgumentUse(0));
        use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "NumericCondition : " + operation;
    }
}
