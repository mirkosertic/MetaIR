package de.mirkosertic.metair.ir;

public class If extends ConditionalNode {

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    If(final Value condition) {
        use(condition, new ArgumentUse(0));

        registerAs(TRUE, controlFlowsTo(new ExtractControlFlowProjection(TRUE), FlowType.FORWARD));
        registerAs(FALSE, controlFlowsTo(new ExtractControlFlowProjection(FALSE), FlowType.FORWARD));
    }

    public ExtractControlFlowProjection trueProjection() {
        return (ExtractControlFlowProjection) getNamedNode(TRUE);
    }

    public ExtractControlFlowProjection falseProjection() {
        return (ExtractControlFlowProjection) getNamedNode(FALSE);
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
