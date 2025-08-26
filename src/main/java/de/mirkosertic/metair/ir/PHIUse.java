package de.mirkosertic.metair.ir;

public class PHIUse extends DataFlowUse {

    public final Node origin;
    public final FlowType type;

    PHIUse(final FlowType type, final Node origin) {
        this.origin = origin;
        this.type = type;
    }
}
