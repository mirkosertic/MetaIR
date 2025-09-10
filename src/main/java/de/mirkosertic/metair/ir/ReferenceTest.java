package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ReferenceTest extends Value {

    public enum Operation {
        NULL, NONNULL
    }

    public final Operation operation;
    public final Value arg0;

    ReferenceTest(final Operation operation, final Value a) {
        super(ConstantDescs.CD_int);

        this.operation = operation;

        this.arg0 = use(a, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ReferenceTest : " + operation;
    }
}
