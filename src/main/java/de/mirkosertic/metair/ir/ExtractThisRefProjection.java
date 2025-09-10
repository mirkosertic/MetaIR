package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class ExtractThisRefProjection extends ConstantValue implements Projection {

    ExtractThisRefProjection(final ClassDesc type, final Method source) {
        super(type);

        use(source, new ArgumentUse(0));
    }

    @Override
    public String name() {
        return "this";
    }

    @Override
    public String debugDescription() {
        return "this : " + TypeUtils.toString(type);
    }
}
