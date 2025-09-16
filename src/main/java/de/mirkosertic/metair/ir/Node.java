package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final Map<ClassDesc, RuntimeclassReference> runtimeclassReferences;

    protected Node() {
        this.uses = new ArrayList<>();
        this.usedBy = new HashSet<>();
        this.runtimeclassReferences = new HashMap<>();
    }

    public Node controlFlowsTo(final Node target, final FlowType type) {
        target.use(this, new ControlFlowUse(type));
        return target;
    }

    protected <T extends Node> T use(final T v, final Use use) {
        uses.add(new UseEdge(v, use));
        v.usedBy.add(this);
        return v;
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

    public PHI definePHI(final ConstantDesc type) {
        final PHI p = new PHI(type);
        p.use(this, DefinedByUse.INSTANCE);
        return p;
    }

    public RuntimeclassReference defineRuntimeclassReference(final ClassDesc type) {
        return runtimeclassReferences.computeIfAbsent(type, key -> {
            final RuntimeclassReference r = new RuntimeclassReference(key);
            r.use(this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public boolean isDataUsedMultipleTimes() {
        int x = 0;
        for (final Node user : usedBy) {
            for (final UseEdge edge : user.uses) {
                if (edge.use() instanceof DataFlowUse && edge.node() == this) {
                    x++;
                }
            }
        }
        return x > 1;
    }

    public List<Node> arguments() {
        return uses.stream().filter(e -> e.use instanceof ArgumentUse).map(x -> x.node).toList();
    }

    public List<Node> definitions() {
        final List<Node> result = new ArrayList<>();
        for (final Node user : usedBy) {
            for (final UseEdge edge : user.uses) {
                if (edge.use() instanceof DefinedByUse && edge.node() == this) {
                    result.add(user);
                }
            }
        }
        return result;
    }

    public record UseEdge(Node node, Use use) {
    }
}