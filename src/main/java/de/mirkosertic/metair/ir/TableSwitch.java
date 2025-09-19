package de.mirkosertic.metair.ir;

import java.util.List;

public class TableSwitch extends ConditionalNode {

    public final int lowValue;
    public final int highValue;
    public final List<Integer> cases;
    public final String defaultLabel;

    TableSwitch(final Value value, final int lowValue, final int highValue, final String defaultLabel, final List<Integer> cases) {

        if (!value.type.equals(IRType.CD_int)) {
            illegalArgument("Cannot use non int value of type " + TypeUtils.toString(value.type) + " as switch value");
        }

        use(value, new ArgumentUse(0));

        this.lowValue = lowValue;
        this.highValue = highValue;
        this.cases = cases;
        this.defaultLabel = defaultLabel;

        for (int i = 0; i < cases.size(); i++) {
            registerAs("case" + i, controlFlowsTo(new ExtractControlFlowProjection("case" + i), FlowType.FORWARD));
        }

        registerAs("default", controlFlowsTo(new ExtractControlFlowProjection("default"), FlowType.FORWARD));
    }

    public ExtractControlFlowProjection defaultProjection() {
        return (ExtractControlFlowProjection) getNamedNode("default");
    }

    public ExtractControlFlowProjection caseProjection(final int index) {
        return (ExtractControlFlowProjection) getNamedNode("case" + index);
    }

    @Override
    public String debugDescription() {
        return "TableSwitch";
    }
}
