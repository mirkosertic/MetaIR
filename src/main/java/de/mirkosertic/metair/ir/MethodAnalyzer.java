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
import java.lang.constant.ConstantDesc;
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

    private ClassDesc owner;
    private MethodModel method;
    private final Map<Label, Integer> labelToIndex;
    private final Method ir;
    private Frame[] frames;
    private List<Frame> codeModelTopologicalOrder;
    private MethodTypeDesc methodTypeDesc;

    MethodAnalyzer() {
        this.ir = new Method();
        this.labelToIndex = new HashMap<>();
    }

    MethodAnalyzer(final MethodTypeDesc methodTypeDesc) {
        this();
        this.methodTypeDesc = methodTypeDesc;
    }

    public MethodAnalyzer(final ClassDesc owner, final MethodModel method) {
        this();
        this.owner = owner;
        this.method = method;
        this.methodTypeDesc = method.methodTypeSymbol();

        final Optional<CodeModel> optCode = method.code();
        if (optCode.isPresent()) {
            try {
                final CodeModel code = optCode.get();
                step1AnalyzeCFG(code);
                step2ComputeTopologicalOrder();
                step3FollowCFGAndInterpret(code);
                step4PeepholeOptimizations();
            } catch (final IllegalParsingStateException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw new IllegalParsingStateException(this, "Unexpected exception", ex);
            }
        }
    }

    protected void illegalState(final String message) {
        final IllegalParsingStateException ex = new IllegalParsingStateException(this, message);
        final StackTraceElement[] old = ex.getStackTrace();
        final StackTraceElement[] newTrace = new StackTraceElement[old.length - 1];
        System.arraycopy(old, 1, newTrace, 0, old.length - 1);
        ex.setStackTrace(newTrace);
        throw ex;
    }

    protected void assertMinimumStackSize(final Status status, final int minStackSize) {
        final int stackSize = status.stack.size();
        if (stackSize < minStackSize) {
            final IllegalParsingStateException ex = new IllegalParsingStateException(this, "A minimum stack size of " + minStackSize + " is required, but only " + stackSize + " is available!");
            final StackTraceElement[] old = ex.getStackTrace();
            final StackTraceElement[] newTrace = new StackTraceElement[old.length - 1];
            System.arraycopy(old, 1, newTrace, 0, old.length - 1);
            ex.setStackTrace(newTrace);
            throw ex;
        }
    }

    protected void assertEmptyStack(final Status status) {
        if (!status.stack.isEmpty()) {
            final IllegalParsingStateException ex = new IllegalParsingStateException(this, "The stack should be empty, but it is not! It still has " + status.stack.size() + " element(s).");
            final StackTraceElement[] old = ex.getStackTrace();
            final StackTraceElement[] newTrace = new StackTraceElement[old.length - 1];
            System.arraycopy(old, 1, newTrace, 0, old.length - 1);
            ex.setStackTrace(newTrace);
            throw ex;
        }
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

        final Status initStatus = new Status(cm.maxLocals());
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
                initStatus.setLocal(localIndex++, p);
            }
            final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                final PHI p = loop.definePHI(argumentTypes[i]);
                p.use(ir.defineMethodArgument(argumentTypes[i], i), new PHIUse(ir));
                initStatus.setLocal(localIndex++, p);
                if (TypeUtils.isCategory2(argumentTypes[i])) {
                    initStatus.setLocal(localIndex++, null);
                }
            }

        } else {
            // We start directly
            int localIndex = 0;
            if (!method.flags().flags().contains(AccessFlag.STATIC)) {
                initStatus.setLocal(localIndex++, ir.defineThisRef(owner));
            }
            final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                initStatus.setLocal(localIndex++, ir.defineMethodArgument(argumentTypes[i], i));
                if (TypeUtils.isCategory2(argumentTypes[i])) {
                    initStatus.setLocal(localIndex++, null);
                }
            }
        }

        topologicalOrder.getFirst().in = initStatus;

        final Map<Integer, Frame> posToFrame = new HashMap<>();
        for (final Frame frame : topologicalOrder) {
            posToFrame.put(frame.elementIndex, frame);
        }

        for (int i = 0; i < topologicalOrder.size(); i++) {
            final Frame frame = topologicalOrder.get(i);

            Status incomingStatus = frame.in;
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
                    if (outgoing.out == null) {
                        illegalState("No outgoing status for " + frame.elementIndex);
                    }
                    incomingStatus = outgoing.out.copy();
                    switch (edge.projection) {
                        case DEFAULT -> {
                            // No nothing in this case, we just keep the incoming control node
                        }
                        case TRUE -> incomingStatus.control = (Node) ((If) incomingStatus.control).trueCase;
                        case FALSE -> incomingStatus.control = (Node) ((If) incomingStatus.control).falseCase;
                        default -> illegalState("Unknown projection type " + edge.projection);
                    }
                    frame.in = incomingStatus;
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
                            if (incomingFrame.out == null) {
                                illegalState("No outgoing status for " + incomingFrame.elementIndex);
                            }
                            final Node memory = incomingFrame.out.memory;
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
                            incomingFrame.out.control.controlFlowsTo(target, ControlType.FORWARD);
                        }
                    }

                    incomingStatus = new Status(cm.maxLocals());

                    for (int mergeLocalIndex = 0; mergeLocalIndex < cm.maxLocals(); mergeLocalIndex++) {
                        final Value source = incomingFrames.getFirst().out.getLocal(mergeLocalIndex);
                        if (source != null) {
                            final List<Value> allValues = new ArrayList<>();
                            allValues.add(source);
                            for (int incomingIndex = 1; incomingIndex < incomingFrames.size(); incomingIndex++) {
                                final Value otherValue = incomingFrames.get(incomingIndex).out.getLocal(mergeLocalIndex);
                                if (otherValue != null && !allValues.contains(otherValue)) {
                                    allValues.add(otherValue);
                                }
                            }
                            if (allValues.size() > 1 || hasBackEdges) {
                                final PHI p = target.definePHI(source.type);
                                for (final Frame incomingFrame : incomingFrames) {
                                    final Value sv = incomingFrame.out.getLocal(mergeLocalIndex);
                                    if (sv == null) {
                                        illegalState("No source value for local " + i + " from " + incomingFrame.elementIndex);
                                    }
                                    p.use(sv, new PHIUse(incomingFrame.out.control));
                                }
                                incomingStatus.setLocal(mergeLocalIndex, p);
                            } else {
                                incomingStatus.setLocal(mergeLocalIndex, allValues.getFirst());
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

                    frame.in = incomingStatus;
                    frame.entryPoint = target;

//                    illegalState("Multi-Join or phi not supported yed!");
                }
            }

            if (frame.entryPoint == null) {
                // This is the thing we need to interpret
                final CodeElement element = frame.codeElement;

                // Interpret the node
                visitNode(element, frame);

                if (frame.out == null || frame.out == incomingStatus) {
                    illegalState("No outgoing or same same as incoming status for " + element);
                }
            } else {
                frame.copyIncomingToOutgoing();
            }
        }
    }

    private void visitNode(final CodeElement node, final Frame frame) {

        if (node instanceof final PseudoInstruction psi) {
            // Pseudo Instructions
            switch (psi) {
                case final LabelTarget labelTarget -> visitLabelTarget(labelTarget, frame);
                case final LineNumber lineNumber -> visitLineNumberNode(lineNumber, frame);
                case final LocalVariable localVariable ->
                    // Maybe we can use this for debugging?
                        frame.out = frame.in.copy();
                case final LocalVariableType localVariableType -> frame.in.copy();
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
                        visitConstantInstruction(constantInstruction.opcode(), constantInstruction.constantValue(), frame);
                case final FieldInstruction fieldInstruction -> visitFieldInstruction(fieldInstruction, frame);
                case final NewObjectInstruction newObjectInstruction ->
                        visitNewObjectInstruction(newObjectInstruction, frame);
                case final ReturnInstruction returnInstruction -> visitReturnInstruction(returnInstruction.opcode(), frame);
                case final InvokeDynamicInstruction invokeDynamicInstruction ->
                        parse_INVOKEDYNAMIC(invokeDynamicInstruction, frame);
                case final TypeCheckInstruction typeCheckInstruction ->
                        visitTypeCheckInstruction(typeCheckInstruction.opcode(), typeCheckInstruction.type().asSymbol(), frame);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction, frame);
                case final OperatorInstruction operatorInstruction ->
                        visitOperatorInstruction(operatorInstruction.opcode(), frame);
                case final MonitorInstruction monitorInstruction ->
                        visitMonitorInstruction(monitorInstruction, frame);
                case final ThrowInstruction thr -> visitThrowInstruction(thr, frame);
                case final NewPrimitiveArrayInstruction na -> visitNewPrimitiveArray(na, frame);
                case final ArrayStoreInstruction as -> visitArrayStoreInstruction(as, frame);
                case final ArrayLoadInstruction al -> visitArrayLoadInstruction(al, frame);
                case final NopInstruction nop -> visitNopInstruction(nop, frame);
                case final NewReferenceArrayInstruction rei -> visitNewObjectArray(rei, frame);
                case final ConvertInstruction ci -> visitConvertInstruction(ci.opcode(), frame);
                case final NewMultiArrayInstruction nm -> visitNewMultiArray(nm, frame);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    @Testbacklog
    protected void visitLabelTarget(final LabelTarget node, final Frame frame) {
        final LabelNode label = ir.createLabel(node.label());
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.control = outgoing.control.controlFlowsTo(label, ControlType.FORWARD);
        frame.entryPoint = label;
    }

    @Testbacklog
    protected void visitLineNumberNode(final LineNumber node, final Frame frame) {
        // A node that represents a line number declaration. These nodes are pseudo-instruction nodes inorder to be inserted in an instruction list.
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.lineNumber = node.line();
    }

    @Testbacklog
    protected void visitExceptionCatch(final ExceptionCatch node, final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    @Testbacklog
    protected void parse_IINC(final IncrementInstruction node, final Frame frame) {
        // A node that represents an IINC instruction.
        frame.copyIncomingToOutgoing();
        frame.out.setLocal(node.slot(), new Add(ConstantDescs.CD_int, frame.in.getLocal(node.slot()), ir.definePrimitiveInt(node.constant())));
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
            case Opcode.GOTO, Opcode.GOTO_W -> parse_GOTO(node, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    protected void visitConstantInstruction(final Opcode opcode, final ConstantDesc constantValue, final Frame frame) {
        switch (opcode) {
            case Opcode.LDC, Opcode.LDC_W, Opcode.LDC2_W -> parse_LDC(constantValue, frame);
            case Opcode.ICONST_M1, Opcode.ICONST_0, Opcode.ICONST_5, Opcode.ICONST_4, Opcode.ICONST_3, Opcode.ICONST_2,
                 Opcode.ICONST_1 -> parse_ICONST(constantValue, frame);
            case Opcode.LCONST_0, Opcode.LCONST_1 -> parse_LCONST(constantValue, frame);
            case Opcode.FCONST_0, Opcode.FCONST_1, Opcode.FCONST_2 -> parse_FCONST(constantValue, frame);
            case Opcode.DCONST_0, Opcode.DCONST_1 -> parse_DCONST(constantValue, frame);
            case Opcode.BIPUSH -> parse_BIPUSH(constantValue, frame);
            case Opcode.SIPUSH -> parse_SIPUSH(constantValue, frame);
            case Opcode.ACONST_NULL -> parse_ACONST_NULL(frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
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

    protected void visitReturnInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.RETURN -> parse_RETURN(frame);
            case Opcode.ARETURN -> parse_ARETURN(frame);
            case Opcode.IRETURN -> parse_RETURN_X(frame, ConstantDescs.CD_int);
            case Opcode.DRETURN -> parse_RETURN_X(frame, ConstantDescs.CD_double);
            case Opcode.FRETURN -> parse_RETURN_X(frame, ConstantDescs.CD_float);
            case Opcode.LRETURN -> parse_RETURN_X(frame, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    @Testbacklog
    protected void parse_INVOKEDYNAMIC(final InvokeDynamicInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, expectedarguments);

        for (int i = 0; i < expectedarguments; i++) {
            final Value v = frame.out.pop();
        }
        if (!returnType.equals(ConstantDescs.CD_void)) {
            // TODO: Create invocation here
            frame.out.push(new Null());
        }
    }

    protected void visitTypeCheckInstruction(final Opcode opcode, final ClassDesc typeToCheck, final Frame frame) {
        assertMinimumStackSize(frame.in, 1);

        switch (opcode) {
            case CHECKCAST -> parse_CHECKCAST(typeToCheck, frame);
            case INSTANCEOF -> parse_INSTANCEOF(typeToCheck, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
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

    protected void visitOperatorInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.IADD, Opcode.DADD, Opcode.FADD, Opcode.LADD, Opcode.FSUB, Opcode.ISUB, Opcode.DSUB,
                 Opcode.LSUB -> visitBinaryOperatorInstruction(opcode, frame);
            case Opcode.DMUL -> parse_MUL_X(frame, ConstantDescs.CD_double);
            case Opcode.FMUL -> parse_MUL_X(frame, ConstantDescs.CD_float);
            case Opcode.IMUL -> parse_MUL_X(frame, ConstantDescs.CD_int);
            case Opcode.LMUL -> parse_MUL_X(frame, ConstantDescs.CD_long);
            case Opcode.ARRAYLENGTH, Opcode.INEG, Opcode.LNEG, Opcode.FNEG, Opcode.DNEG -> visitUnaryOperatorInstruction(opcode, frame);
            case Opcode.IDIV -> parse_DIV_X(frame, ConstantDescs.CD_int);
            case Opcode.LDIV -> parse_DIV_X(frame, ConstantDescs.CD_long);
            case Opcode.FDIV -> parse_DIV_X(frame, ConstantDescs.CD_float);
            case Opcode.DDIV -> parse_DIV_X(frame, ConstantDescs.CD_double);
            case Opcode.IREM -> parse_REM_X(frame, ConstantDescs.CD_int);
            case Opcode.LREM -> parse_REM_X(frame, ConstantDescs.CD_long);
            case Opcode.FREM -> parse_REM_X(frame, ConstantDescs.CD_float);
            case Opcode.DREM -> parse_REM_X(frame, ConstantDescs.CD_double);
            case Opcode.IAND -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.AND);
            case Opcode.IOR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.OR);
            case Opcode.IXOR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.XOR);
            case Opcode.ISHL -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.SHL);
            case Opcode.ISHR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.SHR);
            case Opcode.IUSHR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_int, BitOperation.Operation.USHR);
            case Opcode.LAND -> parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.AND);
            case Opcode.LOR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.OR);
            case Opcode.LXOR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.XOR);
            case Opcode.LSHL -> parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.SHL);
            case Opcode.LSHR -> parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.SHR);
            case Opcode.LUSHR ->
                    parse_BITOPERATION_X(frame, ConstantDescs.CD_long, BitOperation.Operation.USHR);
            case Opcode.FCMPG -> parse_NUMERICCOMPARE_X(frame, NumericCompare.Mode.NAN_IS_1);
            case Opcode.FCMPL -> parse_NUMERICCOMPARE_X(frame, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.DCMPG -> parse_NUMERICCOMPARE_X(frame, NumericCompare.Mode.NAN_IS_1);
            case Opcode.DCMPL -> parse_NUMERICCOMPARE_X(frame, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.LCMP -> parse_NUMERICCOMPARE_X(frame, NumericCompare.Mode.NONFLOATINGPOINT);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitBinaryOperatorInstruction(final Opcode opcode, final Frame frame) {
        assertMinimumStackSize(frame.in, 2);

        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value value2 = outgoing.pop();
        final Value value1 = outgoing.pop();

        switch (opcode) {
            case Opcode.IADD -> parse_ADD_X(frame, value1, value2, ConstantDescs.CD_int);
            case Opcode.DADD -> parse_ADD_X(frame, value1, value2, ConstantDescs.CD_double);
            case Opcode.FADD -> parse_ADD_X(frame, value1, value2, ConstantDescs.CD_float);
            case Opcode.LADD -> parse_ADD_X(frame, value1, value2, ConstantDescs.CD_long);
            case Opcode.DSUB -> parse_SUB_X(frame, value1, value2, ConstantDescs.CD_double);
            case Opcode.FSUB -> parse_SUB_X(frame, value1, value2, ConstantDescs.CD_float);
            case Opcode.ISUB -> parse_SUB_X(frame, value1, value2, ConstantDescs.CD_int);
            case Opcode.LSUB -> parse_SUB_X(frame, value1, value2, ConstantDescs.CD_long);

            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitUnaryOperatorInstruction(final Opcode opcode, final Frame frame) {
        assertMinimumStackSize(frame.in, 1);

        switch (opcode) {
            case Opcode.ARRAYLENGTH -> parse_ARRAYLENGTH(frame);
            case Opcode.INEG -> parse_NEG_X(frame, ConstantDescs.CD_int);
            case Opcode.LNEG -> parse_NEG_X(frame, ConstantDescs.CD_long);
            case Opcode.FNEG -> parse_NEG_X(frame, ConstantDescs.CD_float);
            case Opcode.DNEG -> parse_NEG_X(frame, ConstantDescs.CD_double);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    private void visitMonitorInstruction(final MonitorInstruction ins, final Frame frame) {
        switch (ins.opcode()) {
            case Opcode.MONITORENTER -> parse_MONITORENTER(ins, frame);
            case Opcode.MONITOREXIT -> parse_MONITOREXIT(ins, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    @Testbacklog
    protected void visitThrowInstruction(final ThrowInstruction ins, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        final Throw t = new Throw(v);
        outgoing.control = outgoing.control.controlFlowsTo(t, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(t);
        frame.entryPoint = t;
    }

    @Testbacklog
    protected void visitNewPrimitiveArray(final NewPrimitiveArrayInstruction ins, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value length = frame.out.pop();
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
        final NewArray newArray = new NewArray(type.componentType(), length);
        frame.out.push(newArray);
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

    @Testbacklog
    protected void visitNopInstruction(final NopInstruction ins, final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    @Testbacklog
    protected void visitNewObjectArray(final NewReferenceArrayInstruction ins, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value length = outgoing.pop();
        final ClassDesc type = ins.componentType().asSymbol();
        final NewArray newArray = new NewArray(type, length);
        outgoing.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        frame.entryPoint = newArray;
    }

    protected void visitConvertInstruction(final Opcode opcode, final Frame frame) {
        assertMinimumStackSize(frame.in, 1);

        switch (opcode) {
            case Opcode.I2B -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_int);
            case Opcode.I2C -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_int);
            case Opcode.I2S -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_int);
            case Opcode.I2L -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_long);
            case Opcode.I2F -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_float);
            case Opcode.I2D -> parse_CONVERT_X(frame, ConstantDescs.CD_int, ConstantDescs.CD_double);
            case Opcode.L2I -> parse_CONVERT_X(frame, ConstantDescs.CD_long, ConstantDescs.CD_int);
            case Opcode.L2F -> parse_CONVERT_X(frame, ConstantDescs.CD_long, ConstantDescs.CD_float);
            case Opcode.L2D -> parse_CONVERT_X(frame, ConstantDescs.CD_long, ConstantDescs.CD_double);
            case Opcode.F2I -> parse_CONVERT_X(frame, ConstantDescs.CD_float, ConstantDescs.CD_int);
            case Opcode.F2L -> parse_CONVERT_X(frame, ConstantDescs.CD_float, ConstantDescs.CD_long);
            case Opcode.F2D -> parse_CONVERT_X(frame, ConstantDescs.CD_float, ConstantDescs.CD_double);
            case Opcode.D2I -> parse_CONVERT_X(frame, ConstantDescs.CD_double, ConstantDescs.CD_int);
            case Opcode.D2L -> parse_CONVERT_X(frame, ConstantDescs.CD_double, ConstantDescs.CD_long);
            case Opcode.D2F -> parse_CONVERT_X(frame, ConstantDescs.CD_double, ConstantDescs.CD_float);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    @Testbacklog
    protected void visitNewMultiArray(final NewMultiArrayInstruction ins, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, ins.dimensions());

        ClassDesc type = ins.arrayType().asSymbol();
        final List<Value> dimensions = new ArrayList<>();
        for (int i = 0; i < ins.dimensions(); i++) {
            dimensions.add(outgoing.pop());
            type = type.arrayType();
        }
        final NewMultiArray newMultiArray = new NewMultiArray(type, dimensions);
        outgoing.push(newMultiArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newMultiArray);
        frame.entryPoint = newMultiArray;
    }

    @Testbacklog
    protected void parse_INVOKESPECIAL(final InvokeInstruction node, final Frame frame) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final Status outgoing = frame.copyIncomingToOutgoing();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;

        assertMinimumStackSize(outgoing, expectedarguments);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(outgoing.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(next);
        }

        frame.entryPoint = next;
    }

    @Testbacklog
    protected void parse_INVOKEVIRTUAL(final InvokeInstruction node, final Frame frame) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final Status outgoing = frame.copyIncomingToOutgoing();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;

        assertMinimumStackSize(outgoing, expectedarguments);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invocation);
        }

        frame.entryPoint = invocation;
    }

    @Testbacklog
    protected void parse_INVOKEINTERFACE(final InvokeInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, expectedarguments);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invocation);
        }

        frame.entryPoint = invocation;
    }

    @Testbacklog
    protected void parse_INVOKESTATIC(final InvokeInstruction node, final Frame frame) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, expectedarguments);

        final RuntimeclassReference runtimeClass = ir.defineRuntimeclassReference(node.method().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(runtimeClass);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }
        arguments.add(init);

        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invocation);
        }

        frame.entryPoint = init;
    }

    @Testbacklog
    protected void parse_ALOAD(final LoadInstruction node, final Frame frame) {
        final Value v = frame.in.getLocal(node.slot());
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }

        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(v);
    }

    @Testbacklog
    protected void parse_LOAD_TYPE(final LoadInstruction node, final Frame frame, final ClassDesc type) {
        final Value v = frame.in.getLocal(node.slot());
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(v);
    }

    @Testbacklog
    protected void parse_ASTORE(final StoreInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        frame.out.setLocal(node.slot(), outgoing.pop());
    }

    @Testbacklog
    protected void parse_STORE_TYPE(final StoreInstruction node, final Frame frame, final ClassDesc type) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (!v.type.equals(type)) {
            illegalState("Cannot store non " + type + " value " + v + " for index " + node.slot());
        }
        frame.out.setLocal(node.slot(), v);
    }

    @Testbacklog
    protected void parse_IF_ICMP_OP(final BranchInstruction node, final Frame frame, final NumericCondition.Operation op) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v1, v2);
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    @Testbacklog
    protected void parse_IF_NUMERIC_X(final BranchInstruction node, final Frame frame, final NumericCondition.Operation op) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v, ir.definePrimitiveInt(0));
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    @Testbacklog
    protected void parse_IF_REFERENCETEST_X(final BranchInstruction node, final Frame frame, final ReferenceTest.Operation op) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();

        final ReferenceTest referenceCondition = new ReferenceTest(op, v);
        final If next = new If(referenceCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    @Testbacklog
    protected void parse_IF_ACMP_OP(final BranchInstruction node, final Frame frame, final ReferenceCondition.Operation op) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();

        final ReferenceCondition condition = new ReferenceCondition(op, v1, v2);
        final If next = new If(condition);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }
        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    private void handlePotentialBackedgeFor(final Node jumpSource, final Frame frame, final Frame targetFrame) {
        final Status outgoing = frame.out;
        if (targetFrame.indexInTopologicalOrder < frame.indexInTopologicalOrder) {
            // We are doing a back edge here!
            for (int i = 0; i < outgoing.numberOfLocals(); i++) {
                final Value v = frame.out.getLocal(i);
                final Value target = targetFrame.in.getLocal(i);
                if (!(target instanceof PHI)) {
                    illegalState("Local at index " + i + " is not a PHI value but " + v);
                }
                if (v != null && v != target) {
                    target.use(v, new PHIUse(jumpSource));
                }
            }
            outgoing.control = outgoing.control.controlFlowsTo(targetFrame.entryPoint, ControlType.BACKWARD);
        }
    }

    @Testbacklog
    protected void parse_GOTO(final BranchInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Goto next = new Goto();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    protected void parse_LDC(final ConstantDesc value, final Frame frame) {
        // A node that represents an LDC instruction.
        final Status outgoing = frame.copyIncomingToOutgoing();
        switch (value) {
            case final String str -> outgoing.push(ir.defineStringConstant(str));
            case final Integer i -> outgoing.push(ir.definePrimitiveInt(i));
            case final Long l -> outgoing.push(ir.definePrimitiveLong(l));
            case final Float f -> outgoing.push(ir.definePrimitiveFloat(f));
            case final Double d -> outgoing.push(ir.definePrimitiveDouble(d));
            case final ClassDesc classDesc -> outgoing.push(ir.defineRuntimeclassReference(classDesc));
            case null, default -> illegalState("Cannot parse LDC instruction with value " + value);
        }
    }

    private void parse_ICONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveInt((Integer) node));
    }

    private void parse_LCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveLong((Long) node));
    }

    private void parse_FCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveFloat((Float) node));
    }

    private void parse_DCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveDouble((Double) node));
    }

    private void parse_BIPUSH(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveInt((Integer) node));
    }

    private void parse_SIPUSH(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.definePrimitiveInt(((Number) node).shortValue()));
    }

    private void parse_ACONST_NULL(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(ir.defineNullReference());
    }

    @Testbacklog
    protected void parse_GETFIELD(final FieldInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (v.type.isPrimitive() || v.type.isArray()) {
            illegalState("Cannot load field from non object value " + v);
        }
        final GetField get = new GetField(node, v);
        outgoing.push(get);

        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    @Testbacklog
    protected void parse_PUTFIELD(final FieldInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v = outgoing.pop();
        final Value target = outgoing.pop();

        final PutField put = new PutField(target, node.name().stringValue(), node.typeSymbol(), v);

        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_GETSTATIC(final FieldInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final GetStatic get = new GetStatic(ri, node.name().stringValue(), node.typeSymbol());
        outgoing.push(get);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    @Testbacklog
    protected void parse_PUTSTATIC(final FieldInstruction node, final Frame frame) {
        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final Value v = outgoing.pop();

        final PutStatic put = new PutStatic(ri, node.name().stringValue(), node.typeSymbol(), v);
        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_NEW(final NewObjectInstruction node, final Frame frame) {
        final ClassDesc type = node.className().asSymbol();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();

        final New n = new New(init);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(n);

        outgoing.push(n);
    }

    private void parse_RETURN(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertEmptyStack(outgoing);

        final Return next = new Return();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_ARETURN(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();

        if (TypeUtils.isPrimitive(v.type)) {
            illegalState("Expecting type " + TypeUtils.toString(methodTypeDesc.returnType()) + " on stack, got " + TypeUtils.toString(v.type));
        }

        // TODO: Maybe we should check downcastability according to the type hierarchy

        final ReturnValue next = new ReturnValue(v.type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_RETURN_X(final Frame frame, final ClassDesc type) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (!v.type.equals(type)) {
            illegalState("Expecting type " + TypeUtils.toString(type) + " on stack, got " + TypeUtils.toString(v.type));
        }

        final ReturnValue next = new ReturnValue(type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_CHECKCAST(final ClassDesc typeToCheck, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value objectToCheck = outgoing.peek();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(typeToCheck);

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.control = outgoing.control.controlFlowsTo(new CheckCast(objectToCheck, classInit), ControlType.FORWARD);
    }

    private void parse_INSTANCEOF(final ClassDesc typeToCheck, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value objectToCheck = outgoing.pop();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(typeToCheck);

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.push(new InstanceOf(objectToCheck, classInit));
    }

    @Testbacklog
    protected void parse_DUP(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.peek();
        outgoing.push(v);
    }

    @Testbacklog
    protected void parse_DUP_X1(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();
        outgoing.push(v1);
        outgoing.push(v2);
        outgoing.push(v1);
    }

    @Testbacklog
    protected void parse_DUP_X2(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();
        if (TypeUtils.isCategory2(v2.type) && !TypeUtils.isCategory2(v1.type)) {
            // Form 2
            outgoing.push(v1);
            outgoing.push(v2);
            outgoing.push(v1);
        } else {
            // Form 1
            assertMinimumStackSize(outgoing, 1);

            final Value v3 = outgoing.pop();
            if (TypeUtils.isCategory2(v1.type) || TypeUtils.isCategory2(v2.type) || TypeUtils.isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP_X2");
            }
            outgoing.push(v1);
            outgoing.push(v3);
            outgoing.push(v2);
            outgoing.push(v1);
        }
    }

    @Testbacklog
    protected void parse_DUP2(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v1 = outgoing.pop();
        if (TypeUtils.isCategory2(v1.type)) {
            outgoing.push(v1);
            outgoing.push(v1);
            return;
        }

        assertMinimumStackSize(outgoing, 1);

        final Value v2 = outgoing.pop();
        outgoing.push(v2);
        outgoing.push(v1);
        outgoing.push(v2);
        outgoing.push(v1);
    }

    @Testbacklog
    protected void parse_DUP2_X1(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();
        if (TypeUtils.isCategory2(v1.type) && !TypeUtils.isCategory2(v2.type)) {
            // Form 2
            outgoing.push(v1);
            outgoing.push(v2);
            outgoing.push(v1);
        } else {
            // Form 1
            assertMinimumStackSize(outgoing, 1);

            final Value v3 = outgoing.pop();
            if (TypeUtils.isCategory2(v1.type) || TypeUtils.isCategory2(v2.type) || TypeUtils.isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP2_X1");
            }
            outgoing.push(v2);
            outgoing.push(v1);
            outgoing.push(v3);
            outgoing.push(v2);
            outgoing.push(v1);
        }
    }

    @Testbacklog
    protected void parse_DUP2_X2(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();
        if (TypeUtils.isCategory2(v1.type) && TypeUtils.isCategory2(v2.type)) {
            // Form 4
            outgoing.push(v1);
            outgoing.push(v2);
            outgoing.push(v1);
        } else {
            assertMinimumStackSize(outgoing, 1);

            final Value v3 = outgoing.pop();
            if (!TypeUtils.isCategory2(v1.type) && !TypeUtils.isCategory2(v2.type) || TypeUtils.isCategory2(v3.type)) {
                // Form 3
                outgoing.push(v2);
                outgoing.push(v1);
                outgoing.push(v3);
                outgoing.push(v2);
                outgoing.push(v1);
            } else if (TypeUtils.isCategory2(v1.type) || !TypeUtils.isCategory2(v2.type) && (!TypeUtils.isCategory2(v3.type))) {
                // Form 2
                outgoing.push(v1);
                outgoing.push(v3);
                outgoing.push(v2);
                outgoing.push(v1);
            } else {
                // Form 1
                assertMinimumStackSize(outgoing, 1);

                final Value v4 = outgoing.pop();
                if (TypeUtils.isCategory2(v1.type) || TypeUtils.isCategory2(v2.type) || TypeUtils.isCategory2(v3.type) || TypeUtils.isCategory2(v4.type)) {
                    illegalState("All values must be of category 1 type for DUP2_X1");
                }
                outgoing.push(v2);
                outgoing.push(v1);
                outgoing.push(v4);
                outgoing.push(v3);
                outgoing.push(v2);
                outgoing.push(v1);
            }
        }
    }

    @Testbacklog
    protected void parse_POP(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        outgoing.pop();
    }

    @Testbacklog
    protected void parse_POP2(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (TypeUtils.isCategory2(v.type)) {
            // Form 2
            return;
        }
        // Form 1
        assertMinimumStackSize(outgoing, 1);

        outgoing.pop();
    }

    @Testbacklog
    protected void parse_SWAP(final StackInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        final Value b = outgoing.pop();
        outgoing.push(a);
        outgoing.push(b);
    }

    private void parse_ADD_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        if (!value1.type.equals(desc)) {
            illegalState("Cannot add non " + TypeUtils.toString(desc) + " value " + TypeUtils.toString(value1.type) + " as value1");
        }
        if (!value2.type.equals(desc)) {
            illegalState("Cannot add non " + TypeUtils.toString(desc) + " value " + TypeUtils.toString(value2.type) + " as value2");
        }
        final Add add = new Add(desc, value1, value2);
        frame.out.push(add);
    }

    private void parse_SUB_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        if (!value1.type.equals(desc)) {
            illegalState("Cannot subtract non " + TypeUtils.toString(desc) + " value " + TypeUtils.toString(value1.type) + " as value1");
        }
        if (!value2.type.equals(desc)) {
            illegalState("Cannot subtract non " + TypeUtils.toString(desc) + " value " + TypeUtils.toString(value2.type) + " as value2");
        }
        final Sub sub = new Sub(desc, value1, value2);
        frame.out.push(sub);
    }

    @Testbacklog
    protected void parse_MUL_X(final Frame frame, final ClassDesc desc) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for multiplication");
        }
        final Value b = outgoing.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for multiplication");
        }
        final Mul mul = new Mul(desc, b, a);
        outgoing.push(mul);
    }

    private void parse_ARRAYLENGTH(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value array = outgoing.pop();
        if (!array.type.isArray()) {
            illegalState("Cannot get array length of non array value " + array);
        }
        outgoing.push(new ArrayLength(array));
    }

    private void parse_NEG_X(final Frame frame, final ClassDesc desc) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value a = outgoing.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot negate non " + TypeUtils.toString(desc) + " value " + a + " of type " + TypeUtils.toString(a.type));
        }
        outgoing.push(new Negate(desc, a));
    }

    @Testbacklog
    protected void parse_DIV_X(final Frame frame, final ClassDesc desc) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for division");
        }
        final Value b = outgoing.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for division");
        }
        final Div div = new Div(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(div, ControlType.FORWARD);
        outgoing.push(div);
        frame.entryPoint = div;
    }

    @Testbacklog
    protected void parse_REM_X(final Frame frame, final ClassDesc desc) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for remainder");
        }
        final Value b = outgoing.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for remainder");
        }
        final Rem rem = new Rem(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(rem, ControlType.FORWARD);
        outgoing.push(rem);
    }

    @Testbacklog
    protected void parse_BITOPERATION_X(final Frame frame, final ClassDesc desc, final BitOperation.Operation operation) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for bit operation");
        }
        final Value b = outgoing.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for bit operation");
        }
        final BitOperation rem = new BitOperation(desc, operation, b, a);
        outgoing.push(rem);
    }

    @Testbacklog
    protected void parse_NUMERICCOMPARE_X(final Frame frame, final NumericCompare.Mode mode) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        final Value b = outgoing.pop();
        final NumericCompare compare = new NumericCompare(mode, b, a);
        outgoing.push(compare);
    }

    @Testbacklog
    protected void parse_MONITORENTER(final MonitorInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorEnter(v), ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_MONITOREXIT(final MonitorInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorExit(v), ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_ASTORE_X(final ArrayStoreInstruction node, final Frame frame, final ClassDesc arrayType) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 3);

        final Value value = outgoing.pop();
        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ArrayStore store = new ArrayStore(arrayType, array, index, value);

        outgoing.memory = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_AASTORE(final ArrayStoreInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 3);

        final Value value = outgoing.pop();
        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ArrayStore store = new ArrayStore(array.type.componentType(), array, index, value);

        outgoing.control = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, ControlType.FORWARD);
    }

    @Testbacklog
    protected void parse_ALOAD_X(final ArrayLoadInstruction node, final Frame frame, final ClassDesc arrayType, final ClassDesc elementType) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value index = outgoing.pop();
        final Value array = outgoing.pop();
        final Value value = new ArrayLoad(elementType, arrayType, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, ControlType.FORWARD);
        outgoing.push(value);
    }

    @Testbacklog
    protected void parse_AALOAD(final ArrayLoadInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value index = outgoing.pop();
        final Value array = outgoing.pop();
        final Value value = new ArrayLoad(array.type.componentType(), array.type, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, ControlType.FORWARD);
        outgoing.push(value);
    }

    private void parse_CONVERT_X(final Frame frame, final ClassDesc from, final ClassDesc to) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value value = outgoing.pop();
        if (!value.type.equals(from)) {
            illegalState("Expected a value of type " + from + " but got " + value.type);
        }
        outgoing.push(new Convert(to, value, from));
    }

    public Method ir() {
        return ir;
    }

    private void step4PeepholeOptimizations() {
    }

    protected enum CFGProjection {
        DEFAULT, TRUE, FALSE
    }

    protected record CFGEdge(int fromIndex, CFGProjection projection, ControlType controlType) {
    }

    private record CFGAnalysisJob(int startIndex, List<Integer> path) {
    }

    protected static class Frame {

        protected final CodeElement codeElement;
        protected final List<CFGEdge> predecessors;
        protected final int elementIndex;
        protected int indexInTopologicalOrder;
        protected Node entryPoint;
        protected Status in;
        protected Status out;

        public Frame(final int elementIndex, final CodeElement codeElement) {
            this.predecessors = new ArrayList<>();
            this.elementIndex = elementIndex;
            this.indexInTopologicalOrder = -1;
            this.codeElement = codeElement;
        }

        public Status copyIncomingToOutgoing() {
            out = in.copy();
            return out;
        }
    }

    protected static class Status {

        protected final static int UNDEFINED_LINE_NUMBER = -1;

        protected int lineNumber;
        private final Value[] locals;
        protected final Stack<Value> stack;
        protected Node control;
        protected Node memory;

        protected Status(final int maxLocals) {
            this.locals = new Value[maxLocals];
            this.stack = new Stack<>();
            this.lineNumber = UNDEFINED_LINE_NUMBER;
        }

        protected int numberOfLocals() {
            return locals.length;
        }

        protected Value getLocal(final int slot) {
            if (slot > 0) {
                if (locals[slot - 1] != null && TypeUtils.isCategory2(locals[slot - 1].type)) {
                    // This is an illegal state!
                    throw new IllegalStateException("Slot " + (slot - 1) + " is already set to a category 2 value, so cannot read slot " + slot);
                }
            }
            return locals[slot];
        }

        protected void setLocal(final int slot, final Value value) {
            locals[slot] = value;
            if (slot > 0) {
                if (locals[slot - 1] != null && TypeUtils.isCategory2(locals[slot - 1].type)) {
                    // This is an illegal state!
                    throw new IllegalStateException("Slot " + (slot - 1) + " is already set to a category 2 value, so cannot set slot " + slot);
                }
            }
        }

        protected Status copy() {
            final Status result = new Status(locals.length);
            result.lineNumber = lineNumber;
            System.arraycopy(locals, 0, result.locals, 0, locals.length);
            result.stack.addAll(stack);
            result.control = control;
            result.memory = memory;
            return result;
        }

        protected Value pop() {
            return stack.pop();
        }

        protected Value peek() {
            return stack.peek();
        }

        protected void push(final Value value) {
            stack.push(value);
        }
    }
}
