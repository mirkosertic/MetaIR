package de.mirkosertic.metair.ir;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConvertInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.classfile.instruction.LocalVariableType;
import java.lang.classfile.instruction.MonitorInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.NopInstruction;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.classfile.instruction.StackInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class MethodAnalyzer {

    enum CFGProjection {
        DEFAULT, TRUE, FALSE
    }

    record CFGEdge(int fromIndex, CFGProjection projection, ControlType controlType) {
    }

    private record CFGAnalysisJob(int startIndex, List<Integer> path) {
    }

    static class Frame {

        final CodeElement codeElement;
        final List<CFGEdge> predecessors;
        final int elementIndex;
        int indexInTopologicalOrder;
        Node entryPoint;
        Status incoming;
        Status outgoing;

        public Frame(final int elementIndex, final CodeElement codeElement) {
            this.predecessors = new ArrayList<>();
            this.elementIndex = elementIndex;
            this.indexInTopologicalOrder = -1;
            this.codeElement = codeElement;
        }

        public Status copyIncomingToOutgoing() {
            outgoing = incoming.copy();
            return outgoing;
        }

        public Value getLocal(final int index) {
            return outgoing.locals[index];
        }

        public void setLocal(final int index, final Value value) {
            outgoing.locals[index] = value;
        }

        public Value pop() {
            return outgoing.stack.pop();
        }

        public Value peek() {
            return outgoing.stack.peek();
        }

        public void push(final Value value) {
            outgoing.stack.push(value);
        }

    }

    private final ClassDesc owner;
    private final MethodModel method;
    private Frame[] frames;
    private final Map<Label, Integer> labelToIndex;
    private final Method ir;
    private List<Frame> codeModelTopologicalOrder;

    public MethodAnalyzer(final ClassDesc owner, final MethodModel method) {

        this.owner = owner;
        this.method = method;
        this.ir = new Method();
        this.labelToIndex = new HashMap<>();

        final Optional<CodeModel> optCode = method.code();
        if (optCode.isPresent()) {
            try {
                final CodeModel code = optCode.get();
                step1AnalyzeCFG(code);
                step2ComputeTopologicalOrder();
                step3FollowCFGAndInterpret(code);
                //step4PeepholeOptimizations();
            } catch (final IllegalParsingStateException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw new IllegalParsingStateException(this, "Unexpected exception", ex);
            }
        }
    }

    private void illegalState(final String message) {
        final IllegalParsingStateException ex = new IllegalParsingStateException(this, message);
        final StackTraceElement[] old = ex.getStackTrace();
        final StackTraceElement[] newTrace = new StackTraceElement[old.length - 1];
        System.arraycopy(old, 1, newTrace, 0, old.length - 1);
        ex.setStackTrace(newTrace);
        throw ex;
    }

    MethodModel getMethod() {
        return method;
    }

    Frame[] getFrames() {
        return frames;
    }

    List<Frame> getCodeModelTopologicalOrder() {
        return codeModelTopologicalOrder;
    }

    private void step1AnalyzeCFG(final CodeModel code) {

        final List<CodeElement> codeElements = code.elementList();

        frames = new Frame[codeElements.size()];
        frames[0] = new Frame(0, codeElements.getFirst());

        // We first need to map the labels to the instruction index
        for (int i = 0; i < codeElements.size(); i++) {
            if (codeElements.get(i) instanceof final LabelTarget labelTarget) {
                final Label label = labelTarget.label();
                if (labelToIndex.containsKey(label)) {
                    illegalState("Duplicate label " + label + ", already found at " + labelToIndex.get(label) + " and now at " + i);
                }
                labelToIndex.put(label, i);
            }
        }
        final Queue<CFGAnalysisJob> jobs = new ArrayDeque<>();
        final Set<Integer> visited = new HashSet<>();
        jobs.add(new CFGAnalysisJob(0, new ArrayList<>()));

        while (!jobs.isEmpty()) {
            final CFGAnalysisJob job = jobs.poll();
            if (visited.contains(job.startIndex)) {
                continue;
            }
            final List<Integer> newPath = new ArrayList<>(job.path);
            jobend: for (int i = job.startIndex; i < codeElements.size(); i++) {
                visited.add(i);
                newPath.add(i);
                final CodeElement current = codeElements.get(i);
                if (current instanceof final Instruction instruction) {
                    if (instruction instanceof final BranchInstruction branch) {
                        // This can either be a conditional or unconditional branch
                        if (instruction.opcode() == Opcode.GOTO || instruction.opcode() == Opcode.GOTO_W) {
                            final Label target = branch.target();
                            // Unconditional Branch
                            if (labelToIndex.containsKey(target)) {
                               final int newIndex = labelToIndex.get(target);
                                if (!visited.contains(newIndex)) {
                                    jobs.add(new CFGAnalysisJob(newIndex, newPath));
                                }
                                ControlType controlType = ControlType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    controlType = ControlType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, CFGProjection.DEFAULT, controlType));
                                break;
                            } else {
                                illegalState("Unconditional branch to " + branch.target() + " which is not mapped to an index");
                            }
                        } else {
                            // Conditional branch
                            // We split up in multiple analysis tasks
                            final Label target = branch.target();
                            // Unconditional Branch
                            if (labelToIndex.containsKey(target)) {
                                final int newIndex = labelToIndex.get(target);
                                if (!visited.contains(newIndex)) {
                                    jobs.add(new CFGAnalysisJob(newIndex, newPath));
                                }
                                ControlType controlType = ControlType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    controlType = ControlType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, CFGProjection.TRUE, controlType));
                            } else {
                                illegalState("Unconditional branch to " + branch.target() + " which is not mapped to an index");
                            }
                        }
                    } else {
                        // The following opcode ends the analysis of the current job, as control-flow terminates
                        switch (instruction.opcode()) {
                            case Opcode.IRETURN:
                            case Opcode.ARETURN:
                            case Opcode.FRETURN:
                            case Opcode.LRETURN:
                            case Opcode.DRETURN:
                            case Opcode.RETURN:
                            case Opcode.ATHROW: {
                                break jobend;
                            }
                        }
                    }
                }

                Frame nextFrame = frames[i + 1];
                if (nextFrame == null) {
                    nextFrame = new Frame(i + 1, codeElements.get(i + 1));
                    frames[i + 1] = nextFrame;
                }
                // This is a regular forward flow
                if (current instanceof BranchInstruction) {
                    nextFrame.predecessors.add(new CFGEdge(i, CFGProjection.FALSE, ControlType.FORWARD));
                } else {
                    nextFrame.predecessors.add(new CFGEdge(i, CFGProjection.DEFAULT, ControlType.FORWARD));
                }

                if (visited.contains(i +1)) {
                    break;
                }
            }
        }
    }

    private void step2ComputeTopologicalOrder() {
        // We compute the topological order of the bytecode cfg
        // We later iterate by this order to parse every node
        final List<Frame> reversePostOrder = new ArrayList<>();
        final Deque<Frame> currentPath = new ArrayDeque<>();
        currentPath.add(frames[0]);
        final Set<Frame> marked = new HashSet<>();
        marked.add(frames[0]);
        while(!currentPath.isEmpty()) {
            final Frame currentNode = currentPath.peek();
            final List<Frame> forwardNodes = new ArrayList<>();

            // This could be improved if we have to successor list directly...
            final Frame[] frames = getFrames();
            for (final Frame frame : frames) {
                // Maybe null due to unreachable statements in bytecode
                if (frame != null) {
                    for (final CFGEdge edge : frame.predecessors) {
                        if (edge.fromIndex == currentNode.elementIndex && edge.controlType == ControlType.FORWARD) {
                            forwardNodes.add(frame);
                        }
                    }
                }
            }

            // We sort by index in the code model to make this reproducible...
            forwardNodes.sort(Comparator.comparingInt(o -> o.elementIndex));

            if (!forwardNodes.isEmpty()) {
                boolean somethingFound = false;
                for (final Frame node : forwardNodes) {
                    if (marked.add(node)) {
                        currentPath.push(node);
                        somethingFound = true;
                    }
                }
                if (!somethingFound) {
                    reversePostOrder.add(currentNode);
                    currentPath.pop();
                }
            } else {
                reversePostOrder.add(currentNode);
                currentPath.pop();
            }
        }

        codeModelTopologicalOrder = new ArrayList<>();
        for (int i = reversePostOrder.size() - 1; i >= 0; i--) {
            codeModelTopologicalOrder.add(reversePostOrder.get(i));
        }

        for (int i = 0; i < codeModelTopologicalOrder.size(); i++) {
            codeModelTopologicalOrder.get(i).indexInTopologicalOrder = i;
        }

    }

    private void step3FollowCFGAndInterpret(final CodeModel code) {

        final CodeAttribute cm = (CodeAttribute) code;

        final Status initStatus = new Status();
        initStatus.locals = new Value[cm.maxLocals()];
        initStatus.stack = new Stack<>();
        initStatus.control = ir;
        initStatus.memory = ir;

        // We need the topological (reverse-post-order) of the CFG to traverse
        // the instructions in the right order
        final List<Frame> topologicalOrder = getCodeModelTopologicalOrder();

        // Init locals according to the method signature and their types
        if (!topologicalOrder.getFirst().predecessors.isEmpty()) {
            // We have a cfg to the start, so the start should already be a loop header!
            final LoopHeaderNode loop = ir.createLoop("Loop0");
            initStatus.control = initStatus.control.controlFlowsTo(loop, ControlType.FORWARD);

            // We start directly
            int localIndex = 0;
            if (!method.flags().flags().contains(AccessFlag.STATIC)) {
                final PHI p = loop.definePHI(owner);
                p.use(ir.defineThisRef(owner), new PHIUse(ir));
                initStatus.locals[localIndex++] = p;
            }
            final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                final PHI p = loop.definePHI(argumentTypes[i]);
                p.use(ir.defineMethodArgument(argumentTypes[i], i), new PHIUse(ir));
                initStatus.locals[localIndex++] = p;
                if (needsTwoSlots(argumentTypes[i])) {
                    initStatus.locals[localIndex++] = null;
                }
            }

        } else {
            // We start directly
            int localIndex = 0;
            if (!method.flags().flags().contains(AccessFlag.STATIC)) {
                initStatus.locals[localIndex++] = ir.defineThisRef(owner);
            }
            final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                initStatus.locals[localIndex++] = ir.defineMethodArgument(argumentTypes[i], i);
                if (needsTwoSlots(argumentTypes[i])) {
                    initStatus.locals[localIndex++] = null;
                }
            }
        }

        topologicalOrder.getFirst().incoming = initStatus;

        final Map<Integer, Frame> posToFrame = new HashMap<>();
        for (final Frame frame : topologicalOrder) {
            posToFrame.put(frame.elementIndex, frame);
        }

        for (int i = 0; i < topologicalOrder.size(); i++) {
            final Frame frame = topologicalOrder.get(i);

            Status incomingStatus = frame.incoming;
            if (incomingStatus == null) {
                // We need to compute the incoming status from the predecessors
                if (frame.predecessors.isEmpty()) {
                    illegalState("No predecessors for " + frame.elementIndex);
                } if (frame.predecessors.size() == 1) {
                    final CFGEdge edge = frame.predecessors.getFirst();
                    final Frame outgoing = posToFrame.get(edge.fromIndex);
                    if (outgoing == null) {
                        illegalState("No outgoing frame for " + frame.elementIndex);
                    }
                    if (outgoing.outgoing == null) {
                        illegalState("No outgoing status for " + frame.elementIndex);
                    }
                    incomingStatus = outgoing.outgoing.copy();
                    switch (edge.projection) {
                        case DEFAULT -> {
                            // No nothing in this case, we just keep the incoming control node
                        }
                        case TRUE -> {
                            incomingStatus.control = (Node) ((If) incomingStatus.control).trueCase;
                        }
                        case FALSE -> {
                            incomingStatus.control = (Node) ((If) incomingStatus.control).falseCase;
                        }
                        default -> {
                            illegalState("Unknown projection type " + edge.projection);
                        }
                    }
                    frame.incoming = incomingStatus;
                } else {
                    // Eager PHI creation and memory-edge merging
                    final List<Frame> incomingFrames = new ArrayList<>();
                    final List<Node> incomingMemories = new ArrayList<>();
                    boolean hasBackEdges = false;
                    for (final CFGEdge edge : frame.predecessors) {
                        // We only respect forward control flows, as phi data propagation for backward
                        // edges is handled during node parsing of the source instruction, as
                        // the outgoing status for this node is not computed yet.
                        if (edge.controlType == ControlType.FORWARD) {
                            final Frame incomingFrame = posToFrame.get(edge.fromIndex);
                            if (incomingFrame.outgoing == null) {
                                illegalState("No outgoing status for " + incomingFrame.elementIndex);
                            }
                            final Node memory = incomingFrame.outgoing.memory;
                            if (!incomingMemories.contains(memory)) {
                                incomingMemories.add(memory);
                            }
                            incomingFrames.add(posToFrame.get(edge.fromIndex));
                        } else {
                            hasBackEdges = true;
                        }
                    }

                    final MultiInputNode target;
                    if (hasBackEdges) {
                        target = ir.createLoop("Loop" + frame.elementIndex);
                    } else {
                        target = ir.createMergeNode("Merge" + frame.elementIndex);
                    }

                    for (final CFGEdge edge : frame.predecessors) {
                        // We only respect forward control flows, as phi data propagation for backward
                        // edges is handled during node parsing of the source instruction, as
                        // the outgoing status for this node is not computed yet.
                        if (edge.controlType == ControlType.FORWARD) {
                            final Frame incomingFrame = posToFrame.get(edge.fromIndex);
                            incomingFrame.outgoing.control.controlFlowsTo(target, ControlType.FORWARD);
                        }
                    }

                    incomingStatus = new Status();
                    incomingStatus.stack = new Stack<>();
                    incomingStatus.locals = new Value[cm.maxLocals()];

                    for (int mergeLocalIndex = 0; mergeLocalIndex < cm.maxLocals(); mergeLocalIndex++) {
                        final Value source = incomingFrames.getFirst().outgoing.locals[mergeLocalIndex];
                        if (source != null) {
                            final List<Value> allValues = new ArrayList<>();
                            allValues.add(source);
                            for (int incomingIndex = 1; incomingIndex < incomingFrames.size(); incomingIndex++) {
                                final Value otherValue = incomingFrames.get(incomingIndex).outgoing.locals[mergeLocalIndex];
                                if (otherValue != null && !allValues.contains(otherValue)) {
                                    allValues.add(otherValue);
                                }
                            }
                            if (allValues.size() > 1 || hasBackEdges) {
                                final PHI p = target.definePHI(source.type);
                                for (final Frame incomingFrame : incomingFrames) {
                                    final Value sv = incomingFrame.outgoing.locals[mergeLocalIndex];
                                    if (sv == null) {
                                        illegalState("No source value for local " + i + " from " + incomingFrame.elementIndex);
                                    }
                                    p.use(sv, new PHIUse(incomingFrame.outgoing.control));
                                }
                                incomingStatus.locals[mergeLocalIndex] = p;
                            } else {
                                incomingStatus.locals[mergeLocalIndex] = allValues.getFirst();
                            }
                        }
                    }

                    if (incomingMemories.size() == 1) {
                        incomingStatus.memory = incomingMemories.getFirst();
                    } else {
                        for (final Node memory : incomingMemories) {
                            memory.memoryFlowsTo(target);
                        }
                        incomingStatus.memory = target;
                    }
                    incomingStatus.control = target;

                    frame.incoming = incomingStatus;
                    frame.entryPoint = target;

//                    illegalState("Multi-Join or phi not supported yed!");
                }
            }

            if (frame.entryPoint == null) {
                // This is the thing we need to interpret
                final CodeElement element = frame.codeElement;

                // Interpret the node
                visitNode(element, frame);

                if (frame.outgoing == null || frame.outgoing == incomingStatus) {
                    illegalState("No outgoing or same same as incoming status for " + element);
                }
            } else {
                frame.copyIncomingToOutgoing();
            }
        }
    }

    boolean needsTwoSlots(final ClassDesc type) {
        return type.equals(ConstantDescs.CD_long) || type.equals(ConstantDescs.CD_double);
    }

    private void visitNode(final CodeElement node, final Frame frame) {

        if (node instanceof final PseudoInstruction psi) {
            // Pseudo Instructions
            switch (psi) {
                case final LabelTarget labelTarget -> visitLabelTarget(labelTarget, frame);
                case final LineNumber lineNumber -> visitLineNumberNode(lineNumber, frame);
                case final LocalVariable localVariable ->
                    // Maybe we can use this for debugging?
                        frame.outgoing = frame.incoming.copy();
                case final LocalVariableType localVariableType -> frame.incoming.copy();
                case final ExceptionCatch exceptionCatch -> visitExceptionCatch(exceptionCatch, frame);
                default -> throw new IllegalArgumentException("Not implemented yet : " + psi);
            }
        } else if (node instanceof final Instruction ins) {
            // Real bytecode instructions
            switch (ins) {
                case final IncrementInstruction incrementInstruction -> parse_IINC(incrementInstruction, frame);
                case final InvokeInstruction invokeInstruction -> visitInvokeInstruction(invokeInstruction, frame);
                case final LoadInstruction load -> visitLoadInstruction(load, frame);
                case final StoreInstruction store -> visitStoreInstruction(store, frame);
                case final BranchInstruction branchInstruction -> visitBranchInstruction(branchInstruction, frame);
                case final ConstantInstruction constantInstruction ->
                        visitConstantInstruction(constantInstruction, frame);
                case final FieldInstruction fieldInstruction -> visitFieldInstruction(fieldInstruction, frame);
                case final NewObjectInstruction newObjectInstruction ->
                        visitNewObjectInstruction(newObjectInstruction, frame);
                case final ReturnInstruction returnInstruction -> visitReturnInstruction(returnInstruction, frame);
                case final InvokeDynamicInstruction invokeDynamicInstruction ->
                        parse_INVOKEDYNAMIC(invokeDynamicInstruction, frame);
                case final TypeCheckInstruction typeCheckInstruction ->
                        visitTypeCheckInstruction(typeCheckInstruction, frame);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction, frame);
                case final OperatorInstruction operatorInstruction ->
                        visitOperatorInstruction(operatorInstruction, frame);
                case final MonitorInstruction monitorInstruction ->
                        visitMonitorInstruction(monitorInstruction, frame);
                case final ThrowInstruction thr -> visitThrowInstruction(thr, frame);
                case final NewPrimitiveArrayInstruction na -> visitNewPrimitiveArray(na, frame);
                case final ArrayStoreInstruction as -> visitArrayStoreInstruction(as, frame);
                case final ArrayLoadInstruction al -> visitArrayLoadInstruction(al, frame);
                case final NopInstruction nop -> visitNopInstruction(nop, frame);
                case final NewReferenceArrayInstruction rei -> visitNewObjectArray(rei, frame);
                case final ConvertInstruction ci -> visitConvertInstruction(ci, frame);
                case final NewMultiArrayInstruction nm -> visitNewMultiArray(nm, frame);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitLabelTarget(final LabelTarget node, final Frame frame) {
        final LabelNode label = ir.createLabel(node.label());
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.control = outgoing.control.controlFlowsTo(label, ControlType.FORWARD);
        frame.entryPoint = label;
    }

    private void visitLineNumberNode(final LineNumber node, final Frame frame) {
        // A node that represents a line number declaration. These nodes are pseudo-instruction nodes inorder to be inserted in an instruction list.
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.lineNumber = node.line();
    }

    private void visitExceptionCatch(final ExceptionCatch node, final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    private void parse_IINC(final IncrementInstruction node, final Frame frame) {
        // A node that represents an IINC instruction.
        frame.copyIncomingToOutgoing();
        frame.setLocal(node.slot(), new Add(ConstantDescs.CD_int, frame.incoming.locals[node.slot()], ir.definePrimitiveInt(node.constant())));
    }

    private void visitInvokeInstruction(final InvokeInstruction node, final Frame frame) {
        // A node that represents a method instruction. A method instruction is an instruction that invokes a method.
        switch (node.opcode()) {
            case Opcode.INVOKESPECIAL -> parse_INVOKESPECIAL(node, frame);
            case Opcode.INVOKEVIRTUAL -> parse_INVOKEVIRTUAL(node, frame);
            case Opcode.INVOKEINTERFACE -> parse_INVOKEINTERFACE(node, frame);
            case Opcode.INVOKESTATIC -> parse_INVOKESTATIC(node, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitLoadInstruction(final LoadInstruction node, final Frame frame) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (node.opcode()) {
            case Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_2, Opcode.ALOAD_1, Opcode.ALOAD_0 ->
                    parse_ALOAD(node, frame);
            case Opcode.ILOAD, Opcode.ILOAD_3, Opcode.ILOAD_2, Opcode.ILOAD_1, Opcode.ILOAD_0 ->
                    parse_LOAD_TYPE(node, frame, ConstantDescs.CD_int);
            case Opcode.DLOAD, Opcode.DLOAD_3, Opcode.DLOAD_2, Opcode.DLOAD_1, Opcode.DLOAD_0 ->
                    parse_LOAD_TYPE(node, frame, ConstantDescs.CD_double);
            case Opcode.FLOAD, Opcode.FLOAD_3, Opcode.FLOAD_2, Opcode.FLOAD_1, Opcode.FLOAD_0 ->
                    parse_LOAD_TYPE(node, frame, ConstantDescs.CD_float);
            case Opcode.LLOAD, Opcode.LLOAD_3, Opcode.LLOAD_2, Opcode.LLOAD_1, Opcode.LLOAD_0 ->
                    parse_LOAD_TYPE(node, frame, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitStoreInstruction(final StoreInstruction node, final Frame frame) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (node.opcode()) {
            case Opcode.ASTORE, Opcode.ASTORE_3, Opcode.ASTORE_2, Opcode.ASTORE_1, Opcode.ASTORE_0, Opcode.ASTORE_W ->
                    parse_ASTORE(node, frame);
            case Opcode.ISTORE, Opcode.ISTORE_3, Opcode.ISTORE_2, Opcode.ISTORE_1, Opcode.ISTORE_0, Opcode.ISTORE_W ->
                    parse_STORE_TYPE(node, frame, ConstantDescs.CD_int);
            case Opcode.LSTORE, Opcode.LSTORE_3, Opcode.LSTORE_2, Opcode.LSTORE_1, Opcode.LSTORE_0, Opcode.LSTORE_W ->
                    parse_STORE_TYPE(node, frame, ConstantDescs.CD_long);
            case Opcode.FSTORE, Opcode.FSTORE_0, Opcode.FSTORE_1, Opcode.FSTORE_2, Opcode.FSTORE_3, Opcode.FSTORE_W ->
                    parse_STORE_TYPE(node, frame, ConstantDescs.CD_float);
            case Opcode.DSTORE, Opcode.DSTORE_0, Opcode.DSTORE_1, Opcode.DSTORE_2, Opcode.DSTORE_3, Opcode.DSTORE_W ->
                    parse_STORE_TYPE(node, frame, ConstantDescs.CD_double);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitBranchInstruction(final BranchInstruction node, final Frame frame) {
        // A node that represents a jump instruction. A jump instruction is an instruction that may jump to another instruction.
        switch (node.opcode()) {
            case Opcode.IF_ICMPEQ -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.EQ);
            case Opcode.IF_ICMPNE -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.NE);
            case Opcode.IF_ICMPGE -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.GE);
            case Opcode.IF_ICMPLE -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.LE);
            case Opcode.IF_ICMPLT -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.LT);
            case Opcode.IF_ICMPGT -> parse_IF_ICMP_OP(node, frame, NumericCondition.Operation.GT);
            case Opcode.IFEQ -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.EQ);
            case Opcode.IFNE -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.NE);
            case Opcode.IFLT -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.LT);
            case Opcode.IFGE -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.GE);
            case Opcode.IFGT -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.GT);
            case Opcode.IFLE -> parse_IF_NUMERIC_X(node, frame, NumericCondition.Operation.LE);
            case Opcode.IFNULL -> parse_IF_REFERENCETEST_X(node, frame, ReferenceTest.Operation.NULL);
            case Opcode.IFNONNULL -> parse_IF_REFERENCETEST_X(node, frame, ReferenceTest.Operation.NONNULL);
            case Opcode.IF_ACMPEQ -> parse_IF_ACMP_OP(node, frame, ReferenceCondition.Operation.EQ);
            case Opcode.IF_ACMPNE -> parse_IF_ACMP_OP(node, frame, ReferenceCondition.Operation.NE);
            case Opcode.GOTO -> parse_GOTO(node, frame);
            case Opcode.GOTO_W -> parse_GOTO(node, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitConstantInstruction(final ConstantInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.LDC -> parse_LDC(ins, frame);
            case Opcode.LDC_W -> parse_LDC(ins, frame);
            case Opcode.LDC2_W -> parse_LDC(ins, frame);
            case Opcode.ICONST_M1, Opcode.ICONST_0, Opcode.ICONST_5, Opcode.ICONST_4, Opcode.ICONST_3, Opcode.ICONST_2,
                 Opcode.ICONST_1 -> parse_ICONST(ins, frame);
            case Opcode.LCONST_0, Opcode.LCONST_1 -> parse_LCONST(ins, frame);
            case Opcode.FCONST_0, Opcode.FCONST_1, Opcode.FCONST_2 -> parse_FCONST(ins, frame);
            case Opcode.DCONST_0, Opcode.DCONST_1 -> parse_DCONST(ins, frame);
            case Opcode.BIPUSH -> parse_BIPUSH(ins, frame);
            case Opcode.SIPUSH -> parse_SIPUSH(ins, frame);
            case Opcode.ACONST_NULL -> parse_ACONST_NULL(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitFieldInstruction(final FieldInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case GETFIELD -> parse_GETFIELD(ins, frame);
            case PUTFIELD -> parse_PUTFIELD(ins, frame);
            case GETSTATIC -> parse_GETSTATIC(ins, frame);
            case PUTSTATIC -> parse_PUTSTATIC(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitNewObjectInstruction(final NewObjectInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.NEW -> parse_NEW(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitReturnInstruction(final ReturnInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.RETURN -> parse_RETURN(ins, frame);
            case Opcode.ARETURN -> parse_ARETURN(ins, frame);
            case Opcode.IRETURN -> parse_RETURN_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.DRETURN -> parse_RETURN_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.FRETURN -> parse_RETURN_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.LRETURN -> parse_RETURN_X(ins, frame, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void parse_INVOKEDYNAMIC(final InvokeDynamicInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (frame.incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + frame.incoming.stack.size());
        }
        frame.copyIncomingToOutgoing();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = frame.pop();
        }
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            // TODO: Create invocation here
            frame.push(new Result(returnType));
        }
    }

    private void visitTypeCheckInstruction(final TypeCheckInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case CHECKCAST -> parse_CHECKCAST(ins, frame);
            case INSTANCEOF -> parse_INSTANCEOF(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitStackInstruction(final StackInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.DUP -> parse_DUP(ins, frame);
            case Opcode.DUP_X1 -> parse_DUP_X1(ins, frame);
            case Opcode.DUP_X2 -> parse_DUP_X2(ins, frame);
            case Opcode.DUP2 -> parse_DUP2(ins, frame);
            case Opcode.DUP2_X1 -> parse_DUP2_X1(ins, frame);
            case Opcode.DUP2_X2 -> parse_DUP2_X2(ins, frame);
            case Opcode.POP -> parse_POP(ins, frame);
            case Opcode.POP2 -> parse_POP2(ins, frame);
            case Opcode.SWAP -> parse_SWAP(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitOperatorInstruction(final OperatorInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.IADD -> parse_ADD_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.DADD -> parse_ADD_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.FADD -> parse_ADD_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.LADD -> parse_ADD_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.DSUB -> parse_SUB_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.FSUB -> parse_SUB_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.ISUB -> parse_SUB_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.LSUB -> parse_SUB_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.DMUL -> parse_MUL_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.FMUL -> parse_MUL_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.IMUL -> parse_MUL_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.LMUL -> parse_MUL_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.ARRAYLENGTH -> parse_ARRAYLENGTH(ins, frame);
            case Opcode.INEG -> parse_NEG_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.LNEG -> parse_NEG_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.FNEG -> parse_NEG_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.DNEG -> parse_NEG_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.IDIV -> parse_DIV_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.LDIV -> parse_DIV_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.FDIV -> parse_DIV_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.DDIV -> parse_DIV_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.IREM -> parse_REM_X(ins, frame, ConstantDescs.CD_int);
            case Opcode.LREM -> parse_REM_X(ins, frame, ConstantDescs.CD_long);
            case Opcode.FREM -> parse_REM_X(ins, frame, ConstantDescs.CD_float);
            case Opcode.DREM -> parse_REM_X(ins, frame, ConstantDescs.CD_double);
            case Opcode.IAND -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.AND);
            case Opcode.IOR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.OR);
            case Opcode.IXOR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.XOR);
            case Opcode.ISHL -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.SHL);
            case Opcode.ISHR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.SHR);
            case Opcode.IUSHR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_int, BitOperation.Operation.USHR);
            case Opcode.LAND -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.AND);
            case Opcode.LOR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.OR);
            case Opcode.LXOR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.XOR);
            case Opcode.LSHL -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.SHL);
            case Opcode.LSHR -> parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.SHR);
            case Opcode.LUSHR ->
                    parse_BITOPERATION_X(ins, frame, ConstantDescs.CD_long, BitOperation.Operation.USHR);
            case Opcode.FCMPG -> parse_NUMERICCOMPARE_X(ins, frame, NumericCompare.Mode.NAN_IS_1);
            case Opcode.FCMPL -> parse_NUMERICCOMPARE_X(ins, frame, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.DCMPG -> parse_NUMERICCOMPARE_X(ins, frame, NumericCompare.Mode.NAN_IS_1);
            case Opcode.DCMPL -> parse_NUMERICCOMPARE_X(ins, frame, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.LCMP -> parse_NUMERICCOMPARE_X(ins, frame, NumericCompare.Mode.NONFLOATINGPOINT);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitMonitorInstruction(final MonitorInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.MONITORENTER -> parse_MONITORENTER(ins, frame);
            case Opcode.MONITOREXIT -> parse_MONITOREXIT(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitThrowInstruction(final ThrowInstruction ins, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        final Throw t = new Throw(v);
        outgoing.control = outgoing.control.controlFlowsTo(t, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(t);
        frame.entryPoint = t;
    }

    private void visitNewPrimitiveArray(final NewPrimitiveArrayInstruction ins, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        frame.copyIncomingToOutgoing();
        final Status outgoing = frame.outgoing;
        final Value length = frame.pop();
        final ClassDesc type;
        switch (ins.typeKind()) {
            case BYTE -> type = ConstantDescs.CD_byte.arrayType();
            case SHORT -> type = ConstantDescs.CD_short.arrayType();
            case CHAR -> type = ConstantDescs.CD_char.arrayType();
            case INT -> type = ConstantDescs.CD_int.arrayType();
            case LONG -> type = ConstantDescs.CD_long.arrayType();
            case FLOAT -> type = ConstantDescs.CD_float.arrayType();
            case DOUBLE -> type = ConstantDescs.CD_double.arrayType();
            default ->
                    throw new IllegalArgumentException("Not implemented type kind for array creation " + ins.typeKind());
        }
        final NewArray newArray = new NewArray(type, length);
        frame.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        frame.entryPoint = newArray;
    }

    private void visitArrayStoreInstruction(final ArrayStoreInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.BASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_byte.arrayType());
            case Opcode.CASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_char.arrayType());
            case Opcode.SASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_short.arrayType());
            case Opcode.IASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_int.arrayType());
            case Opcode.LASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_long.arrayType());
            case Opcode.FASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_float.arrayType());
            case Opcode.DASTORE -> parse_ASTORE_X(ins, frame, ConstantDescs.CD_double.arrayType());
            case Opcode.AASTORE -> parse_AASTORE(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitArrayLoadInstruction(final ArrayLoadInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.BALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_byte.arrayType(), ConstantDescs.CD_int); // Sign extend!
            case Opcode.CALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_char.arrayType(), ConstantDescs.CD_int); // Zero extend!
            case Opcode.SALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_short.arrayType(), ConstantDescs.CD_int); // Sign extend!
            case Opcode.IALOAD -> parse_ALOAD_X(ins, frame, ConstantDescs.CD_int.arrayType(), ConstantDescs.CD_int);
            case Opcode.LALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_long.arrayType(), ConstantDescs.CD_long);
            case Opcode.FALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_float.arrayType(), ConstantDescs.CD_float);
            case Opcode.DALOAD ->
                    parse_ALOAD_X(ins, frame, ConstantDescs.CD_double.arrayType(), ConstantDescs.CD_double);
            case Opcode.AALOAD -> parse_AALOAD(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitNopInstruction(final NopInstruction ins, final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    private void visitNewObjectArray(final NewReferenceArrayInstruction ins, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        frame.copyIncomingToOutgoing();
        final Status outgoing = frame.outgoing;
        final Value length = frame.pop();
        final ClassDesc type = ins.componentType().asSymbol().arrayType();
        final NewArray newArray = new NewArray(type, length);
        frame.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        frame.entryPoint = newArray;
    }

    private void visitConvertInstruction(final ConvertInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.I2B -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_byte);
            case Opcode.I2C -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_char);
            case Opcode.I2S -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_short);
            case Opcode.I2L -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_long);
            case Opcode.I2F -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_float);
            case Opcode.I2D -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_int, ConstantDescs.CD_double);
            case Opcode.L2I -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_long, ConstantDescs.CD_int);
            case Opcode.L2F -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_long, ConstantDescs.CD_float);
            case Opcode.L2D -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_long, ConstantDescs.CD_double);
            case Opcode.F2I -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_float, ConstantDescs.CD_int);
            case Opcode.F2L -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_float, ConstantDescs.CD_long);
            case Opcode.F2D -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_float, ConstantDescs.CD_double);
            case Opcode.D2I -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_double, ConstantDescs.CD_int);
            case Opcode.D2L -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_double, ConstantDescs.CD_long);
            case Opcode.D2F -> parse_CONVERT_X(ins, frame, ConstantDescs.CD_double, ConstantDescs.CD_float);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitNewMultiArray(final NewMultiArrayInstruction ins, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        frame.copyIncomingToOutgoing();
        final Status outgoing = frame.outgoing;
        ClassDesc type = ins.arrayType().asSymbol();
        final List<Value> dimensions = new ArrayList<>();
        for (int i = 0; i < ins.dimensions(); i++) {
            dimensions.add(frame.pop());
            type = type.arrayType();
        }
        final NewMultiArray newMultiArray = new NewMultiArray(type, dimensions);
        frame.push(newMultiArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newMultiArray);
        frame.entryPoint = newMultiArray;
    }

    private void parse_INVOKESPECIAL(final InvokeInstruction node, final Frame frame) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        frame.copyIncomingToOutgoing();
        final Status outgoing = frame.outgoing;

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (outgoing.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + frame.incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(frame.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            frame.push(next);
        }

        frame.entryPoint = next;
    }

    private void parse_INVOKEVIRTUAL(final InvokeInstruction node, final Frame frame) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        frame.copyIncomingToOutgoing();
        final Status outgoing = frame.outgoing;

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (outgoing.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + frame.incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = frame.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            frame.push(invocation);
        }

        frame.entryPoint = invocation;
    }

    private void parse_INVOKEINTERFACE(final InvokeInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (frame.incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + frame.incoming.stack.size());
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = frame.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            frame.push(invocation);
        }

        frame.entryPoint = invocation;
    }

    private void parse_INVOKESTATIC(final InvokeInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (frame.incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + frame.incoming.stack.size());
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final RuntimeclassReference runtimeClass = ir.defineRuntimeclassReference(node.method().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(runtimeClass);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = frame.pop();
            arguments.add(v);
        }
        arguments.add(init);

        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            frame.push(invocation);
        }

        frame.entryPoint = init;
    }

    private void parse_ALOAD(final LoadInstruction node, final Frame frame) {
        final Value v = frame.incoming.locals[node.slot()];
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }

        frame.copyIncomingToOutgoing();
        frame.push(v);
    }

    private void parse_LOAD_TYPE(final LoadInstruction node, final Frame frame, final ClassDesc type) {
        final Value v = frame.incoming.locals[node.slot()];
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }
        frame.copyIncomingToOutgoing();
        frame.push(v);
    }

    private void parse_ASTORE(final StoreInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot store empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        frame.setLocal(node.slot(), frame.pop());
        if (node.slot() > 0) {
            if (outgoing.locals[node.slot() - 1] != null && needsTwoSlots(outgoing.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                outgoing.locals[node.slot() - 1] = null;
            }
        }
    }

    private void parse_STORE_TYPE(final StoreInstruction node, final Frame frame, final ClassDesc type) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot store empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v = frame.pop();
        if (!v.type.equals(type)) {
            illegalState("Cannot store non " + type + " value " + v + " for index " + node.slot());
        }
        frame.setLocal(node.slot(), v);
        if (node.slot() > 0) {
            if (outgoing.locals[node.slot() - 1] != null && needsTwoSlots(outgoing.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                outgoing.locals[node.slot() - 1] = null;
            }
        }
    }

    private void parse_IF_ICMP_OP(final BranchInstruction node, final Frame frame, final NumericCondition.Operation op) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for comparison");
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v1 = frame.pop();
        final Value v2 = frame.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v1, v2);
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;
    }

    private void parse_IF_NUMERIC_X(final BranchInstruction node, final Frame frame, final NumericCondition.Operation op) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Need a value on stack for comparison");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v, ir.definePrimitiveInt(0));
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;
    }

    private void parse_IF_REFERENCETEST_X(final BranchInstruction node, final Frame frame, final ReferenceTest.Operation op) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Need a value on stack for comparison");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();

        final ReferenceTest referenceCondition = new ReferenceTest(op, v);
        final If next = new If(referenceCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;
    }

    private void parse_IF_ACMP_OP(final BranchInstruction node, final Frame frame, final ReferenceCondition.Operation op) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for comparison");
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v1 = frame.pop();
        final Value v2 = frame.pop();

        final ReferenceCondition condition = new ReferenceCondition(op, v1, v2);
        final If next = new If(condition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;
    }

    private void parse_GOTO(final BranchInstruction node, final Frame frame) {
        // Check if we are doing a back edge
        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Goto next = new Goto();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);

        if (targetFrame.indexInTopologicalOrder < frame.indexInTopologicalOrder) {
            // We are doing a back edge here!
            for (int i = 0; i < outgoing.locals.length; i++) {
                final Value v = frame.getLocal(i);
                final Value target = targetFrame.incoming.locals[i];
                if (!(target instanceof PHI)) {
                    illegalState("Local at index " + i + " is not a PHI value but " + v);
                }
                if (v != null && v != target) {
                    target.use(v, new PHIUse(next));
                    frame.setLocal(i, target);
                }
            }
            outgoing.control = outgoing.control.controlFlowsTo(targetFrame.entryPoint, ControlType.BACKWARD);
        }

        frame.entryPoint = next;
    }

    private void parse_LDC(final ConstantInstruction node, final Frame frame) {
        // A node that represents an LDC instruction.
        frame.copyIncomingToOutgoing();
        if (node.constantValue() instanceof final String str) {
            frame.push(ir.defineStringConstant(str));
        } else if (node.constantValue() instanceof final Integer i) {
            frame.push(ir.definePrimitiveInt(i));
        } else if (node.constantValue() instanceof final Long l) {
            frame.push(ir.definePrimitiveLong(l));
        } else if (node.constantValue() instanceof final Float f) {
            frame.push(ir.definePrimitiveFloat(f));
        } else if (node.constantValue() instanceof final Double d) {
            frame.push(ir.definePrimitiveDouble(d));
        } else if (node.constantValue() instanceof final ClassDesc classDesc) {
            frame.push(ir.defineRuntimeclassReference(classDesc));
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void parse_ICONST(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveInt((Integer) node.constantValue()));
    }

    private void parse_LCONST(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveLong((Long) node.constantValue()));
    }

    private void parse_FCONST(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveFloat((Float) node.constantValue()));
    }

    private void parse_DCONST(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveDouble((Double) node.constantValue()));
    }

    private void parse_BIPUSH(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveInt((Integer) node.constantValue()));
    }

    private void parse_SIPUSH(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.definePrimitiveShort(((Number) node.constantValue()).shortValue()));
    }

    private void parse_ACONST_NULL(final ConstantInstruction node, final Frame frame) {
        frame.copyIncomingToOutgoing();
        frame.push(ir.defineNullReference());
    }

    private void parse_GETFIELD(final FieldInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot load field from empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        if (v.type.isPrimitive() || v.type.isArray()) {
            illegalState("Cannot load field from non object value " + v);
        }
        final GetField get = new GetField(node, v);
        frame.push(get);

        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    private void parse_PUTFIELD(final FieldInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot put field from empty stack");
        }

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v = frame.pop();
        final Value target = frame.pop();

        final PutField put = new PutField(target, node.name().stringValue(), node.typeSymbol(), v);

        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
    }

    private void parse_GETSTATIC(final FieldInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final GetStatic get = new GetStatic(ri, node.name().stringValue(), node.typeSymbol());
        frame.push(get);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    private void parse_PUTSTATIC(final FieldInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot put field from empty stack");
        }

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final Value v = frame.pop();

        final PutStatic put = new PutStatic(ri, node.name().stringValue(), node.typeSymbol(), v);
        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
    }

    private void parse_NEW(final NewObjectInstruction node, final Frame frame) {
        final ClassDesc type = node.className().asSymbol();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();

        final New n = new New(init);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(n);

        frame.push(n);
    }

    private void parse_RETURN(final ReturnInstruction node, final Frame frame) {
        final Return next = new Return();

        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_ARETURN(final ReturnInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot return empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();

        final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
        if (!v.type.equals(methodTypeDesc.returnType())) {
            illegalState("Expecting type " + methodTypeDesc.returnType() + " on stack, got " + v.type);
        }

        final ReturnValue next = new ReturnValue(v.type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_RETURN_X(final ReturnInstruction node, final Frame frame, final ClassDesc type) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot return empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        final ReturnValue next = new ReturnValue(type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_CHECKCAST(final TypeCheckInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Checkcast requires a stack entry");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value objectToCheck = frame.peek();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(node.type().asSymbol());

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.control = outgoing.control.controlFlowsTo(new CheckCast(objectToCheck, classInit), ControlType.FORWARD);
    }

    private void parse_INSTANCEOF(final TypeCheckInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Instanceof requires a stack entry");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value objectToCheck = frame.pop();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(node.type().asSymbol());

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        frame.push(new InstanceOf(objectToCheck, classInit));
    }

    private void parse_DUP(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        frame.copyIncomingToOutgoing();
        final Value v = frame.peek();
        frame.push(v);
    }

    private void parse_DUP_X1(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Two stack values are required for DUP_X1");
        }
        frame.copyIncomingToOutgoing();
        final Value v1 = frame.peek();
        final Value v2 = frame.peek();
        frame.push(v1);
        frame.push(v2);
        frame.push(v1);
    }

    private void parse_DUP_X2(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Two stack values are required for DUP_X2");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v1 = frame.pop();
        final Value v2 = frame.pop();
        if (isCategory2(v2.type) && !isCategory2(v1.type)) {
            // Form 2
            frame.push(v1);
            frame.push(v2);
            frame.push(v1);
        } else {
            // Form 1
            if (outgoing.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP_X2");
            }
            final Value v3 = frame.pop();
            if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP_X2");
            }
            frame.push(v1);
            frame.push(v3);
            frame.push(v2);
            frame.push(v1);
        }
    }

    private void parse_DUP2(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v1 = frame.pop();
        if (isCategory2(v1.type)) {
            frame.push(v1);
            frame.push(v1);
            return;
        }
        if (outgoing.stack.isEmpty()) {
            illegalState("Another stack entry is required for DUP2 type 1 operation");
        }
        final Value v2 = frame.pop();
        frame.push(v2);
        frame.push(v1);
        frame.push(v2);
        frame.push(v1);
    }

    private void parse_DUP2_X1(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("A minium of two stack values are required for DUP2_X1");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v1 = frame.pop();
        final Value v2 = frame.pop();
        if (isCategory2(v1.type) && !isCategory2(v2.type)) {
            // Form 2
            frame.push(v1);
            frame.push(v2);
            frame.push(v1);
        } else {
            // Form 1
            if (outgoing.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP2_X1");
            }
            final Value v3 = frame.pop();
            if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP2_X1");
            }
            frame.push(v2);
            frame.push(v1);
            frame.push(v3);
            frame.push(v2);
            frame.push(v1);
        }
    }

    private void parse_DUP2_X2(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("A minium of two stack values are required for DUP2_X1");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value v1 = frame.pop();
        final Value v2 = frame.pop();
        if (isCategory2(v1.type) && isCategory2(v2.type)) {
            // Form 4
            frame.push(v1);
            frame.push(v2);
            frame.push(v1);
        } else {
            if (outgoing.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP2_X1");
            }
            final Value v3 = frame.pop();
            if (!isCategory2(v1.type) && !isCategory2(v2.type) || isCategory2(v3.type)) {
                // Form 3
                frame.push(v2);
                frame.push(v1);
                frame.push(v3);
                frame.push(v2);
                frame.push(v1);
            } else if (isCategory2(v1.type) || !isCategory2(v2.type) && (!isCategory2(v3.type))) {
                // Form 2
                frame.push(v1);
                frame.push(v3);
                frame.push(v2);
                frame.push(v1);
            } else {
                // Form 1
                if (outgoing.stack.isEmpty()) {
                    illegalState("Another stack entry is required for DUP2_X1");
                }
                final Value v4 = outgoing.stack.pop();
                if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type) || isCategory2(v4.type)) {
                    illegalState("All values must be of category 1 type for DUP2_X1");
                }
                frame.push(v2);
                frame.push(v1);
                frame.push(v4);
                frame.push(v3);
                frame.push(v2);
                frame.push(v1);
            }
        }
    }

    private void parse_POP(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot pop from empty stack");
        }
        frame.copyIncomingToOutgoing();
        frame.pop();
    }

    private void parse_POP2(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot pop from empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        if (isCategory2(v.type)) {
            // Form 2
            return;
        }
        // Form 1
        if (outgoing.stack.isEmpty()) {
            illegalState("Another type 1 entry is required on stack to pop!");
        }
        frame.pop();
    }

    private void parse_SWAP(final StackInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Expected at least two values on stack for swap");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        final Value b = frame.pop();
        frame.push(a);
        frame.push(b);
    }

    private void parse_ADD_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for addition");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for addition");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for addition");
        }
        final Add add = new Add(desc, b, a);
        frame.push(add);
    }

    private void parse_SUB_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for substraction");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for substraction");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for substraction");
        }
        final Sub sub = new Sub(desc, b, a);
        frame.push(sub);
    }

    private void parse_MUL_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for multiplication");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for multiplication");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for multiplication");
        }
        final Mul mul = new Mul(desc, b, a);
        frame.push(mul);
    }

    private void parse_ARRAYLENGTH(final OperatorInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Array on stack is required!");
        }
        frame.copyIncomingToOutgoing();
        final Value array = frame.pop();
        frame.push(new ArrayLength(array));
    }

    private void parse_NEG_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Need a minium of one value on stack for negation");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot negate non " + desc + " value " + a + " of type " + a.type);
        }
        frame.push(new Negate(desc, a));
    }

    private void parse_DIV_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for division");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for division");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for division");
        }
        final Div div = new Div(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(div, ControlType.FORWARD);
        frame.push(div);
        frame.entryPoint = div;
    }

    private void parse_REM_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for remainder");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for remainder");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for remainder");
        }
        final Rem rem = new Rem(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(rem, ControlType.FORWARD);
        frame.push(rem);
    }

    private void parse_BITOPERATION_X(final OperatorInstruction node, final Frame frame, final ClassDesc desc, final BitOperation.Operation operation) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for bit operation");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for bit operation");
        }
        final Value b = frame.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for bit operation");
        }
        final BitOperation rem = new BitOperation(desc, operation, b, a);
        frame.push(rem);
    }

    private void parse_NUMERICCOMPARE_X(final OperatorInstruction node, final Frame frame, final NumericCompare.Mode mode) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for numeric comparison");
        }
        frame.copyIncomingToOutgoing();
        final Value a = frame.pop();
        final Value b = frame.pop();
        final NumericCompare compare = new NumericCompare(mode, b, a);
        frame.push(compare);
    }

    private void parse_MONITORENTER(final MonitorInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorEnter(v), ControlType.FORWARD);
    }

    private void parse_MONITOREXIT(final MonitorInstruction node, final Frame frame) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value v = frame.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorExit(v), ControlType.FORWARD);
    }

    private void parse_ASTORE_X(final ArrayStoreInstruction node, final Frame frame, final ClassDesc arrayType) {
        if (frame.incoming.stack.size() < 3) {
            illegalState("Three stack entries required for array store");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value value = frame.pop();
        final Value index = frame.pop();
        final Value array = frame.pop();

        final ArrayStore store = new ArrayStore(arrayType, array, index, value);

        outgoing.memory = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, ControlType.FORWARD);
    }

    private void parse_AASTORE(final ArrayStoreInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 3) {
            illegalState("Three stack entries required for array store");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value value = frame.pop();
        final Value index = frame.pop();
        final Value array = frame.pop();

        final ArrayStore store = new ArrayStore(array.type.componentType(), array, index, value);

        outgoing.control = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, ControlType.FORWARD);
    }

    private void parse_ALOAD_X(final ArrayLoadInstruction node, final Frame frame, final ClassDesc arrayType, final ClassDesc elementType) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Two stack entries required for array store");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value index = frame.pop();
        final Value array = frame.pop();
        final Value value = new ArrayLoad(elementType, arrayType, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, ControlType.FORWARD);
        frame.push(value);
    }

    private void parse_AALOAD(final ArrayLoadInstruction node, final Frame frame) {
        if (frame.incoming.stack.size() < 2) {
            illegalState("Two stack entries required for array store");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value index = frame.pop();
        final Value array = frame.pop();
        final Value value = new ArrayLoad(array.type.componentType(), array.type, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, ControlType.FORWARD);
        frame.push(value);
    }

    private void parse_CONVERT_X(final ConvertInstruction node, final Frame frame, final ClassDesc from, final ClassDesc to) {
        if (frame.incoming.stack.isEmpty()) {
            illegalState("Expected an entry on the stack for type conversion");
        }
        frame.copyIncomingToOutgoing();
        final Value value = frame.pop();
        if (!value.type.equals(from)) {
            illegalState("Expected a value of type " + from + " but got " + value.type);
        }
        frame.push(new Convert(to, value, from));
    }

    private static boolean isCategory2(final ClassDesc desc) {
        return ConstantDescs.CD_long.equals(desc) || ConstantDescs.CD_double.equals(desc);
    }

    public Method ir() {
        return ir;
    }

    private void step4PeepholeOptimizations() {
        ir.peepholeOptimizations();
    }

    static class Status {
        int lineNumber = -1;
        Value[] locals;
        Stack<Value> stack;
        Node control;
        Node memory;

        Status copy() {
            final Status result = new Status();
            result.lineNumber = lineNumber;
            result.locals = new Value[locals.length];
            System.arraycopy(locals, 0, result.locals, 0, locals.length);
            result.stack = new Stack<>();
            result.stack.addAll(stack);
            result.control = control;
            result.memory = memory;
            return result;
        }
    }
}
