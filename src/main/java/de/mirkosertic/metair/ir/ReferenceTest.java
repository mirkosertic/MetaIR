package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;

public class ReferenceTest extends Value {

    public enum Operation {
        NULL, NONNULL
    }

    public final Operation operation;

    ReferenceTest(final Operation operation, final Value a) {
        super(ConstantDescs.CD_boolean);

        this.operation = operation;

        use(a, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ReferenceTest : " + operation;
    }
}
