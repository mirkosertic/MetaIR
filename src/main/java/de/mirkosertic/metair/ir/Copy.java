package de.mirkosertic.metair.ir;

import java.util.Optional;

public class Copy extends Node {

    Copy(final Value from, final Value to) {
        use(from, new DataFlowUse());
        to.use(this, new DataFlowUse());
    }

    @Override
    public boolean isConstant() {
        final Optional<Node> argument = uses.stream().filter(t -> t.use() instanceof DataFlowUse).map(t -> t.node()).findFirst();
        return argument.map(Node::isConstant).orElse(false);
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
