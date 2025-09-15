package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDesc;
import java.util.List;

public class PHI extends Value {

    PHI(final ConstantDesc type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "Φ " + TypeUtils.toString(type);
    }

    @Override
    public boolean isConstant() {
        final List<UseEdge> dataflows = uses.stream().filter(t -> t.use() instanceof DataFlowUse).toList();
        if (dataflows.isEmpty()) {
            return false;
        }
        for (final UseEdge edge : dataflows) {
            if (!edge.node().isConstant()) {
                return false;
            }
        }
        return true;
    }

    public Node initExpressionFor(final Node node) {
        for (final UseEdge edge : uses) {
            if (edge.use() instanceof final PHIUse phiUse && phiUse.origin == node) {
                return edge.node();
            }
        }
        return null;
    }
}
