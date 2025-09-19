package de.mirkosertic.metair.ir;

public class ReferenceCondition extends Value {

    public enum Operation {
        EQ, NE
    }

    public final Operation operation;

    ReferenceCondition(final Operation operation, final Value a, final Value b) {
        super(IRType.CD_int);

        this.operation = operation;

        use(a, new ArgumentUse(0));
        use(b, new ArgumentUse(1));
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "ReferenceCondition : " + operation;
    }
}
