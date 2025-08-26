package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDescs;
import java.util.List;

public class LookupSwitch extends TupleNode {

    public final List<Integer> cases;
    public final String defaultLabel;

    LookupSwitch(final Value value, final String defaultLabel, final List<Integer> cases) {

        if (!value.type.equals(ConstantDescs.CD_int)) {
            illegalArgument("Cannot use non int value of type " + TypeUtils.toString(value.type) + " as switch value");
        }

        use(value, new ArgumentUse(0));

        this.cases = cases;
        this.defaultLabel = defaultLabel;

        for (int i = 0; i < cases.size(); i++) {
            registerAs("case" + i, controlFlowsTo(new ExtractControlFlowProjection("case" + i), FlowType.FORWARD));
        }

        registerAs("default", controlFlowsTo(new ExtractControlFlowProjection("default"), FlowType.FORWARD));
    }

    @Override
    public String debugDescription() {
        return "LookupSwitch";
    }
}
