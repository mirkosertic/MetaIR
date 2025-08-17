package de.mirkosertic.metair.ir;

import java.lang.constant.ConstantDesc;

public class ExtractMethodArgProjection extends Value implements Projection {

    private final int index;

    ExtractMethodArgProjection(final ConstantDesc type, final Method source, final int index) {
        super(type);
        this.index = index;

        use(source, new ArgumentUse(index));
    }

    public int index() {
        return index;
    }

    @Override
    public String name() {
        return "arg" + index;
    }

    @Override
    public String debugDescription() {
        return "arg" + index + " : " + TypeUtils.toString(type);
    }
}
