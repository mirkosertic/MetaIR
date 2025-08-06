package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ReferenceCondition extends Value {

    public enum Operation {
        EQ, NE
    }

    public final Operation operation;

    ReferenceCondition(final Operation operation, final Value a, final Value b) {
        super(ConstantDescs.CD_int);

        this.operation = operation;

        use(a, new ArgumentUse(0));
        use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "ReferenceCondition : " + operation;
    }
}
