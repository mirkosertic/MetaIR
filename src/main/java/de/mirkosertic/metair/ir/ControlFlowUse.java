package de.mirkosertic.metair.ir;

public class ControlFlowUse extends Use {

    public ControlType type;
    public final ControlFlowCondition condition;

    ControlFlowUse(final ControlType type, final ControlFlowCondition condition) {
        this.type = type;
        this.condition = condition;
    }
}
