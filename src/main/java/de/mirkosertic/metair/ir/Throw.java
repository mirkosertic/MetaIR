package de.mirkosertic.metair.ir;

public class Throw extends Node {

    Throw(final Value object) {
        use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
