package de.mirkosertic.metair.ir;

public class If extends ConditionalNode {

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    If(final Value condition) {
        use(condition, new ArgumentUse(0));

        registerAs(TRUE, controlFlowsTo(new ExtractControlFlowProjection(TRUE), ControlType.FORWARD));
        registerAs(FALSE, controlFlowsTo(new ExtractControlFlowProjection(FALSE), ControlType.FORWARD));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
