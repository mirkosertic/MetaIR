package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public abstract class MultiInputNode extends Node {

    public PHI definePHI(final ClassDesc type) {
        final PHI p = new PHI(type);
        p.use(this, DefinedByUse.INSTANCE);
        return p;
    }
}
