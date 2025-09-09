package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;

public class Catch extends TupleNode {

    public final List<ClassDesc> exceptionTypes;

    Catch(final List<ClassDesc> exceptionTypes, final Node source) {

        this.exceptionTypes = exceptionTypes;

        use(source, new ArgumentUse(0));

        registerAs("exception", new CaughtExceptionProjection(this));
    }

    public Value caughtException() {
        return (Value) getNamedNode("exception");
    }

    @Override
    public String debugDescription() {
        final StringBuilder result = new StringBuilder("Catch : ");
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
}
