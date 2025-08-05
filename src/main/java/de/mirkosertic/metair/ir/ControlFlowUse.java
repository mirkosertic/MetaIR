package de.mirkosertic.metair.ir;

public class ControlFlowUse extends Use {

    public final ControlType type;

    ControlFlowUse(final ControlType type) {
        this.type = type;
    }
}
