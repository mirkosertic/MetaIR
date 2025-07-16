package de.mirkosertic.metair.ir;

import org.objectweb.asm.Type;

import java.util.*;

public class Label extends Node {

    public final String label;

    Label(final String label) {
        this.label = label;
    }

    public PHI definePHI(final Type type) {
        final PHI p = new PHI(type);
        p.use(this, DefinedByUse.INSTANCE);
        return p;
    }

    @Override
    public Set<Node> peepholeOptimization() {
        final List<UseEdge> incomingControlFlows = uses.stream().filter(t -> t.use instanceof ControlFlowUse).toList();
        final Map<Node, List<UseEdge>> controlflowsTo = outgoingControlFlows();

        for (final Map.Entry<Node, List<UseEdge>> e : controlflowsTo.entrySet()) {
            for (final UseEdge u : e.getValue()) {
                if (((ControlFlowUse) u.use).type == ControlType.BACKWARD) {
                    // We do not support optimization of labels with back edges for now...
                    return Collections.emptySet();
                }
            }
        }

        if (incomingControlFlows.size() == 1 && controlflowsTo.size() == 1) {

            final Node incoming = incomingControlFlows.getFirst().node;
            final Node outgoing = controlflowsTo.keySet().iterator().next();

            incoming.usedBy.remove(this);
            incoming.usedBy.add(outgoing);

            for (final UseEdge o : outgoing.uses) {
                if (o.node == this) {
                    o.node = incoming;
                }
            }

            uses.clear();
            usedBy.clear();

            final Set<Node> result = new HashSet<>();
            result.addAll(incomingControlFlows.stream().map(t -> t.node).toList());
            result.addAll(controlflowsTo.keySet());
            return result;

            //kill();
        }

        return Collections.emptySet();
    }
}
