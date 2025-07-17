package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.Optional;

public class ReturnValue extends Value {

    ReturnValue(final ClassDesc type, final Value value) {
        super(type);
        use(value, new ArgumentUse(0));
    }

    @Override
    public boolean isConstant() {
        final Optional<Node> argument = uses.stream().filter(t -> t.use instanceof DataFlowUse).map(t -> t.node).findFirst();
        return argument.map(Node::isConstant).orElse(false);
    }

    @Override
    public String debugDescription() {
        return "ReturnValue : " + DebugUtils.toString(type);
    }
}
