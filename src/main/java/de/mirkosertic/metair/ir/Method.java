package de.mirkosertic.metair.ir;


import java.lang.classfile.Label;
import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Method extends Node {

    private final Map<Label, LabelNode> labelMap;
    private final Map<ClassDesc, RuntimeclassReference> runtimeclassReferences;
    private final Map<Object, Value> constants;
    private Null nullref;

    Method() {
        this.labelMap = new HashMap<>();
        this.runtimeclassReferences = new HashMap<>();
        this.constants = new HashMap<>();
        this.nullref = null;
    }

    public RuntimeclassReference defineRuntimeclassReference(final ClassDesc type) {
        return runtimeclassReferences.computeIfAbsent(type, key -> {
            final RuntimeclassReference r = new RuntimeclassReference(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public Null defineNullReference() {
        if (nullref == null) {
            nullref = new Null();
            nullref.use(this, DefinedByUse.INSTANCE);
        }
        return nullref;
    }

    public StringConstant defineStringConstant(final String value) {
        return (StringConstant) constants.computeIfAbsent(value, key -> {
            final StringConstant r = new StringConstant(value);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public ExtractThisRefProjection defineThisRef(final ClassDesc type) {
        return (ExtractThisRefProjection) controlFlowsTo(new ExtractThisRefProjection(type), ControlType.FORWARD);
    }

    public ExtractMethodArgProjection defineMethodArgument(final ClassDesc type, final int index) {
        return (ExtractMethodArgProjection) controlFlowsTo(new ExtractMethodArgProjection(type, index), ControlType.FORWARD);
    }

    public MergeNode createMergeNode(final String label) {
        return new MergeNode(label);
    }

    public LoopHeaderNode createLoop(final String label) {
        return new LoopHeaderNode(label);
    }

    public LabelNode createLabel(final Label label) {
        return labelMap.computeIfAbsent(label, key -> new LabelNode(label.toString()));
    }

    public PrimitiveInt definePrimitiveInt(final int value) {
        return (PrimitiveInt) constants.computeIfAbsent(value, key -> {
            final PrimitiveInt v = new PrimitiveInt(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveLong definePrimitiveLong(final long value) {
        return (PrimitiveLong) constants.computeIfAbsent(value, key -> {
            final PrimitiveLong v = new PrimitiveLong(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveByte definePrimitiveByte(final byte value) {
        return (PrimitiveByte) constants.computeIfAbsent(value, key -> {
            final PrimitiveByte v = new PrimitiveByte(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveShort definePrimitiveShort(final short value) {
        return (PrimitiveShort) constants.computeIfAbsent(value, key -> {
            final PrimitiveShort v = new PrimitiveShort(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveFloat definePrimitiveFloat(final float value) {
        return (PrimitiveFloat) constants.computeIfAbsent(value, key -> {
            final PrimitiveFloat v = new PrimitiveFloat(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public PrimitiveDouble definePrimitiveDouble(final double value) {
        return (PrimitiveDouble) constants.computeIfAbsent(value, key -> {
            final PrimitiveDouble v = new PrimitiveDouble(value);
            v.use(Method.this, DefinedByUse.INSTANCE);
            return v;
        });
    }

    public void peepholeOptimizations() {
        final Queue<Node> workingQueue = new LinkedList<>();
        workingQueue.add(this);

        final Set<Node> visited = new HashSet<>();

        while (!workingQueue.isEmpty()) {
            final Node n = workingQueue.poll();
            workingQueue.addAll(n.peepholeOptimization());
            visited.add(n);

            for (final Node user : n.usedBy) {
                if (!visited.contains(user)) {
                    workingQueue.add(user);
                }
            }
        }
    }

    @Override
    public String debugDescription() {
        return getClass().getSimpleName();
    }
}
