package de.mirkosertic.metair.ir;

public class ControlFlowUse extends Use {

    public final FlowType type;

    ControlFlowUse(final FlowType type) {
        this.type = type;
    }
}
