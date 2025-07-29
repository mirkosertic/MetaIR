package de.mirkosertic.metair.ir;

public class PHIUse extends DataFlowUse {

    public final Node origin;

    PHIUse(final Node origin) {
        this.origin = origin;
    }
}
