package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;

public class PHI extends Value {

    PHI(final ClassDesc type) {
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
}
