package de.mirkosertic.metair.ir;

public class Goto extends Node {

    Goto() {
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }

    public Node getJumpTarget() {
        for (final Node user : usedBy) {
            for (final UseEdge edge : user.uses) {
                if (edge.node() == this && edge.use() instanceof ControlFlowUse) {
                    return user;
                }
            }
        }
        throw new IllegalStateException("Cannot find the jump target");
    }
}
