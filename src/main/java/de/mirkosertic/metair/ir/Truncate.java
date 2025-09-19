package de.mirkosertic.metair.ir;

public class Truncate extends Value {

    Truncate(final IRType.MetaClass targetType, final Value value) {
        super(targetType);

        use(value, new ArgumentUse(0));
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Truncate : " + TypeUtils.toString(type);
    }
}
