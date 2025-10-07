package de.mirkosertic.metair.ir;

public class PHI extends Value {

    PHI(final IRType<?> type) {
        super(type);
    }

    @Override
    public String debugDescription() {
        return "Φ " + TypeUtils.toString(type);
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
