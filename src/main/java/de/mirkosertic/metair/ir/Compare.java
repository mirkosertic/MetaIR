package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

public class Compare extends Value {

    public enum Operation {
        EQ, NE, LT, GE, GT, LE
    }

    public final Operation operation;

    Compare(final Operation operation, final Value a, final Value b) {
        super(Type.BOOLEAN_TYPE);

        this.operation = operation;

        use(a, new ArgumentUse(0));
        use(b, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return "Compare : " + operation;
    }
}
