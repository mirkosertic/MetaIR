package de.mirkosertic.metair.ir;

public class RuntimeclassReference extends Value {

    RuntimeclassReference(final IRType<?> type) {
        super(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Class " + TypeUtils.toString(type);
    }
}
