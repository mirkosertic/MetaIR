package de.mirkosertic.metair.ir;

public class MonitorExit extends Node {

    MonitorExit(final Value object) {
        use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
