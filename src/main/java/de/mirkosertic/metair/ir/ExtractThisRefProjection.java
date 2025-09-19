package de.mirkosertic.metair.ir;

public class ExtractThisRefProjection extends ConstantValue implements Projection {

    ExtractThisRefProjection(final IRType.MetaClass type, final Method source) {
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
