package de.mirkosertic.metair.ir;

public class MonitorEnter extends Node {

    public final Value arg0;

    MonitorEnter(final Value object) {

        if (object.isPrimitive()) {
            illegalArgument("Expecting non primitive type for monitorenter on stack, got " + TypeUtils.toString(object.type));
        }

        this.arg0 = use(object, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
