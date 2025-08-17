package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class Catch extends TupleNode {

    public final ClassDesc exceptionType;

    Catch(final ClassDesc exceptionType, final Node source) {

        this.exceptionType = exceptionType;

        use(source, new ArgumentUse(0));

        registerAs("exception", new CaughtExceptionProjection(this));
    }

    public Value caughtException() {
        return (Value) getNamedNode("exception");
    }

    @Override
    public String debugDescription() {
        return "Catch : " + TypeUtils.toString(exceptionType);
    }
}
