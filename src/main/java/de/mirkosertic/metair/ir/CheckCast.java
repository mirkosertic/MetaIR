package de.mirkosertic.metair.ir;

public class CheckCast extends Value {

    CheckCast(final Value arg1, final Value arg2) {
        super(arg1.type);
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
