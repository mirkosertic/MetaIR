package de.mirkosertic.metair.ir;

public class LabelNode extends Node {

    public final String label;

    LabelNode(final String label) {
        this.label = label;
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
