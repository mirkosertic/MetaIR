package de.mirkosertic.metair.ir;

public class Convert extends Value {

    public final IRType.MetaClass from;

    Convert(final IRType.MetaClass to, final Value arg1, final IRType.MetaClass from) {
        super(to);

        if (!arg1.type.equals(from)) {
            illegalArgument("Expected a value of type " + TypeUtils.toString(from) + " but got " + TypeUtils.toString(arg1.type));
        }

        use(arg1, new ArgumentUse(0));
        this.from = from;
    }

    @Override
    public boolean sideeffectFree() {
        return true;
    }

    @Override
    public String debugDescription() {
        return "Convert : " + TypeUtils.toString(from) + " to " + TypeUtils.toString(type);
    }
}
