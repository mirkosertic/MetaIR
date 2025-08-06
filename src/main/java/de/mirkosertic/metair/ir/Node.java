package de.mirkosertic.metair.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Node {

    // Incoming uses
    protected final List<UseEdge> uses;
    protected final Set<Node> usedBy;

    protected Node() {
        this.uses = new ArrayList<>();
        this.usedBy = new HashSet<>();
    }

    public Node controlFlowsTo(final Node target, final ControlType type) {
        target.use(this, new ControlFlowUse(type));
        return target;
    }

    protected void use(final Node v, final Use use) {
        uses.add(new UseEdge(v, use));
        v.usedBy.add(this);
    }

    public Node memoryFlowsTo(final Node target) {
        target.use(this, MemoryUse.INSTANCE);
        return target;
    }

    @Override
    public String toString() {
        return debugDescription();
    }

    public abstract String debugDescription();

    public boolean isConstant() {
        return false;
    }

    public record UseEdge(Node node, Use use) {
    }

}
