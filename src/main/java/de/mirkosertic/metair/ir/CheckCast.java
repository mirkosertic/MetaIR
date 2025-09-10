package de.mirkosertic.metair.ir;

public class CheckCast extends Node {

    public final Value arg0;
    public final Value arg1;

    CheckCast(final Value arg1, final Value arg2) {
        this.arg0 = use(arg1, new ArgumentUse(0));
        this.arg1 = use(arg2, new ArgumentUse(1));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
