package de.mirkosertic.metair.ir;

public class LoopHeaderNode extends MultiInputNode {

    public final String label;

    LoopHeaderNode(final String label) {
        this.label = label;
    }

    @Override
    public String debugDescription() {
        return "LoopHeader : " + label;
    }
}
