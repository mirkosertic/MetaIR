package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ReferenceCondition extends Value {

    public enum Operation {
        EQ, NE
    }

    public final Operation operation;
    public final Value arg0;
    public final Value arg1;

    ReferenceCondition(final Operation operation, final Value a, final Value b) {
        super(ConstantDescs.CD_int);

        this.operation = operation;

        this.arg0 = use(a, new ArgumentUse(0));
        this.arg1 = use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "ReferenceCondition : " + operation;
    }
}
