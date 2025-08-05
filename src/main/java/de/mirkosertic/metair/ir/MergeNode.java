package de.mirkosertic.metair.ir;

public class MergeNode extends MultiInputNode {

    public final String label;

    MergeNode(final String label) {
        this.label = label;
    }

    @Override
    public String debugDescription() {
        return "Merge : " + label;
    }
}
