package de.mirkosertic.metair.ir;


import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.util.*;

public class Method extends Node {

    private final Map<Label, LabelNode> labelMap;
    private final MethodModel methodNode;
    private final Map<ClassDesc, RuntimeclassReference> runtimeclassReferences;
    private final Map<String, StringConstant> stringConstants;

    Method(final MethodModel methodNode) {
        this.labelMap = new HashMap<>();
        this.methodNode = methodNode;
        this.runtimeclassReferences = new HashMap<>();
        this.stringConstants = new HashMap<>();
    }

    public RuntimeclassReference defineRuntimeclassReference(final ClassDesc type) {
        return runtimeclassReferences.computeIfAbsent(type, key -> {
            final RuntimeclassReference r = new RuntimeclassReference(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public StringConstant defineStringConstant(final String value) {
        return stringConstants.computeIfAbsent(value, key -> {
            final StringConstant r = new StringConstant(key);
            r.use(Method.this, DefinedByUse.INSTANCE);
            return r;
        });
    }

    public ThisRef defineThisRef(final ClassDesc type) {
        final ThisRef t = new ThisRef(type);
        t.use(this, DefinedByUse.INSTANCE);
        return t;
    }

    public MethodArgument defineMethodArgument(final ClassDesc type, final int index) {
        final MethodArgument a = new MethodArgument(type, index);
        a.use(this, DefinedByUse.INSTANCE);
        return a;
    }

    public LabelNode createLabel(final Label label) {
        return labelMap.computeIfAbsent(label, key -> new LabelNode(label.toString()));
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

    public PrimitiveByte definePrimitiveByte(final int value) {
        final PrimitiveByte v = new PrimitiveByte(value);
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

    public void markBackEdges() {
        final Queue<Node> woringQueue = new LinkedList<>();
        final Set<Node> visited = new HashSet<>();

        woringQueue.add(this);
        while (!woringQueue.isEmpty()) {
            final Node node = woringQueue.poll();
            visited.add(node);

            // TODO: Special handling for switch/case constructs required?
            final Map<Node, List<UseEdge>> controlFlows = node.outgoingControlFlows();

            for (final Map.Entry<Node, List<UseEdge>> controlFlow : controlFlows.entrySet()) {
                if (visited.contains(controlFlow.getKey())) {
                    for (final UseEdge edge : controlFlow.getValue()) {
                        ((ControlFlowUse) edge.use).type = ControlType.BACKWARD;
                    }
                } else {
                    woringQueue.add(controlFlow.getKey());
                }
            }
        }
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
