package de.mirkosertic.metair.ir;

public class ControlFlowConditionOnTrue extends ControlFlowCondition {

    public static final ControlFlowConditionOnTrue INSTANCE = new ControlFlowConditionOnTrue();

    private ControlFlowConditionOnTrue() {
    }

    @Override
    public String debugDescription() {
        return "on true";
    }
}
