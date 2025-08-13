package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDesc;

public abstract class MultiInputNode extends Node {

    public PHI definePHI(final ConstantDesc type) {
        final PHI p = new PHI(type);
        p.use(this, DefinedByUse.INSTANCE);
        return p;
    }
}
