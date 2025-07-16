package de.mirkosertic.metair.ir;

import java.util.*;

public abstract class Node {

    public static class UseEdge {
        public Node node;
        public final Use use;

        UseEdge(final Node node, final Use use) {
            this.node = node;
            this.use = use;
        }
    }

    // Incoming uses
    protected final List<UseEdge> uses;

    protected final Set<Node> usedBy;

    protected Node() {
        this.uses = new ArrayList<>();
        this.usedBy = new HashSet<>();
    }

    protected void use(final Node v, final Use use) {
        uses.add(new UseEdge(v, use));
        v.usedBy.add(this);
    }

    public Node controlFlowsTo(final Node target, final ControlType type) {
        return controlFlowsTo(target, type, ControlFlowConditionDefault.INSTANCE);
    }

    public Node controlFlowsTo(final Node target, final ControlType type, final ControlFlowCondition condition) {
        target.use(this, new ControlFlowUse(type, condition));
        return target;
    }

    public void kill() {
        for (final UseEdge edge : uses) {
            edge.node.usedBy.remove(this);
        }
        uses.clear();
    }

    public String debugDescription() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return debugDescription();
    }

    public Map<Node, List<UseEdge>> outgoingControlFlows() {
        final Map<Node, List<UseEdge>> controlFlows = new HashMap<>();
        for (final Node usedBy : usedBy) {
            for (final UseEdge edge : usedBy.uses.stream().filter(t -> t.node == this && t.use instanceof ControlFlowUse).toList()) {
                controlFlows.computeIfAbsent(usedBy, k -> new ArrayList<>()).add(edge);
            }
        }
        return controlFlows;
    }

    public Set<Node> peepholeOptimization() {
        return Collections.emptySet();
    }

    public boolean isConstant() {
        return false;
    }

}
