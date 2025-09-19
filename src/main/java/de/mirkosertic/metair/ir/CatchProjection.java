package de.mirkosertic.metair.ir;

import java.util.List;

public class CatchProjection extends Node implements Projection {

    public final int index;
    public final List<IRType.MetaClass> exceptionTypes;

    public static String nameFor(final int index, final List<IRType.MetaClass> exceptionTypes) {
        final StringBuilder result = new StringBuilder("catch : ");
        result.append(index);
        result.append(" : ");
        for (int i = 0; i < exceptionTypes.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(TypeUtils.toString(exceptionTypes.get(i)));
        }
        if (exceptionTypes.isEmpty()) {
            result.append("any");
        }
        return result.toString();
    }

    CatchProjection(final int index, final List<IRType.MetaClass> types) {
        this.index = index;
        this.exceptionTypes = types;
    }

    @Override
    public String name() {
        return nameFor(index, exceptionTypes);
    }

    @Override
    public String debugDescription() {
        return name();
    }
}
