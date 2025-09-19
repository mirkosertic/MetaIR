package de.mirkosertic.metair.ir;

public class LabelNode extends Node {

    public final String label;

    LabelNode(final String label) {
        this.label = label;
    }

    public PHI definePHI(final IRType.MetaClass type) {
        final PHI p = new PHI(type);
        p.use(this, DefinedByUse.INSTANCE);
        return p;
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
