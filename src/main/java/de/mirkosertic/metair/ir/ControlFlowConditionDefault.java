package de.mirkosertic.metair.ir;

public class ControlFlowConditionDefault extends ControlFlowCondition {

    public static final ControlFlowConditionDefault INSTANCE = new ControlFlowConditionDefault();

    private ControlFlowConditionDefault() {
    }

    @Override
    public String debugDescription() {
        return "default";
    }
}
