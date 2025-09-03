package de.mirkosertic.metair.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Node {

    protected static void illegalArgument(final String message) {
        final RuntimeException ex = new IllegalArgumentException(message);
        final StackTraceElement[] old = ex.getStackTrace();
        final StackTraceElement[] newTrace = new StackTraceElement[old.length - 1];
        System.arraycopy(old, 1, newTrace, 0, old.length - 1);
        ex.setStackTrace(newTrace);
        throw ex;
    }

    // Incoming uses
    protected final List<UseEdge> uses;
    protected final Set<Node> usedBy;

    protected Node() {
        this.uses = new ArrayList<>();
        this.usedBy = new HashSet<>();
    }

    public Node controlFlowsTo(final Node target, final FlowType type) {
        target.use(this, new ControlFlowUse(type));
        return target;
    }

    protected void use(final Node v, final Use use) {
        uses.add(new UseEdge(v, use));
        v.usedBy.add(this);
    }

    public Node memoryFlowsTo(final Node target) {
        target.use(this, MemoryUse.INSTANCE);
        return target;
    }

    @Override
    public String toString() {
        return debugDescription();
    }

    public abstract String debugDescription();

    public boolean isConstant() {
        return false;
    }

    public Null defineNullReference() {
        final Null nullref = new Null();
        nullref.use(this, DefinedByUse.INSTANCE);
        return nullref;
    }

    public StringConstant defineStringConstant(final String value) {
        final StringConstant r = new StringConstant(value);
        r.use(this, DefinedByUse.INSTANCE);
        return r;
    }

    public PrimitiveInt definePrimitiveInt(final int value) {
        final PrimitiveInt v = new PrimitiveInt(value);
        v.use(this, DefinedByUse.INSTANCE);
        return v;
    }

    public PrimitiveLong definePrimitiveLong(final long value) {
        final PrimitiveLong v = new PrimitiveLong(value);
        v.use(this, DefinedByUse.INSTANCE);
        return v;
    }

    public PrimitiveFloat definePrimitiveFloat(final float value) {
        final PrimitiveFloat v = new PrimitiveFloat(value);
        v.use(this, DefinedByUse.INSTANCE);
        return v;
    }

    public PrimitiveDouble definePrimitiveDouble(final double value) {
        final PrimitiveDouble v = new PrimitiveDouble(value);
        v.use(this, DefinedByUse.INSTANCE);
        return v;
    }

    public record UseEdge(Node node, Use use) {
    }
}