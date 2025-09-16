package de.mirkosertic.metair.ir;

public class CheckCast extends Node {

    CheckCast(final Value arg1, final Value arg2) {
        use(arg1, new ArgumentUse(0));
        use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
