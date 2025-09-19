package de.mirkosertic.metair.ir;

public class ExtractMethodArgProjection extends ConstantValue implements Projection {

    private final int index;

    ExtractMethodArgProjection(final IRType type, final Method source, final int index) {
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
