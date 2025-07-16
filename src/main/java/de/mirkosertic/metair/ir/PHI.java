package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

import java.util.*;

public class PHI extends Value {

    PHI(final Type type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "Φ " + type;
    }

    @Override
    public boolean isConstant() {
        final List<UseEdge> dataflows = uses.stream().filter(t -> t.use instanceof DataFlowUse).toList();
        if (dataflows.isEmpty()) {
            return false;
        }
        for (final UseEdge edge : dataflows) {
            if (!edge.node.isConstant()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Node> peepholeOptimization() {
        final Set<Node> dataflowSources = new HashSet<>();
        for (final UseEdge edge : uses) {
            if (edge.use instanceof DataFlowUse) {
                dataflowSources.add(edge.node);
            }
        }
        if (usedBy.isEmpty()) {
            if (isConstant()) {

                kill();

                return dataflowSources;
            }
        }
        return Collections.emptySet();
    }
}
