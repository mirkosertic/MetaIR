package de.mirkosertic.metair.ir;

public class If extends ConditionalNode {

    public final Projection trueCase;
    public final Projection falseCase;

    If(final Value condition) {
        use(condition, new ArgumentUse(0));

        this.trueCase = (Projection) controlFlowsTo(new ExtractControlFlowProjection("true"), ControlType.FORWARD);
        this.falseCase = (Projection) controlFlowsTo(new ExtractControlFlowProjection("false"), ControlType.FORWARD);
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
