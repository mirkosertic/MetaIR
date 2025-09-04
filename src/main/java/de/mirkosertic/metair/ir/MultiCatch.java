package de.mirkosertic.metair.ir;

public class MultiCatch extends TupleNode {

    public final String label;

    MultiCatch(final String label) {
        this.label = label;

        registerAs("exception", new CaughtExceptionProjection(this));
    }

    public Value caughtException() {
        return (Value) getNamedNode("exception");
    }

    @Override
    public String debugDescription() {
        return "MultiCatch : " + label;
    }
}
