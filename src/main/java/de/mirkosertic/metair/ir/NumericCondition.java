package de.mirkosertic.metair.ir;

public class NumericCondition extends Value {

    public enum Operation {
        EQ, NE, LT, GE, GT, LE
    }

    public final Operation operation;

    NumericCondition(final Operation operation, final Value a, final Value b) {
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
        return "NumericCondition : " + operation;
    }
}
