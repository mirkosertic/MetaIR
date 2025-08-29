package de.mirkosertic.metair.ir;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.CharacterRange;
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
import java.lang.classfile.instruction.LookupSwitchInstruction;
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
import java.lang.classfile.instruction.SwitchCase;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
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
import java.util.stream.Collectors;

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
        this(method.methodTypeSymbol());
        this.owner = owner;
        this.method = method;

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
                throw new IllegalParsingStateException(this, ex.getMessage(), ex);
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

    public MethodModel getMethod() {
        return method;
    }

    public Frame[] getFrames() {
        return frames;
    }

    public List<Frame> getCodeModelTopologicalOrder() {
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
            jobend:
            for (int i = job.startIndex; i < codeElements.size(); i++) {
                visited.add(i);
                newPath.add(i);
                final CodeElement current = codeElements.get(i);
                if (current instanceof final LabelTarget lt) {
                    final Label label = lt.label();
                    // Search for exception handlers
                    for (int exceptionIndex = 0; exceptionIndex < code.exceptionHandlers().size(); exceptionIndex++) {
                        final ExceptionCatch c = code.exceptionHandlers().get(exceptionIndex);

                        if (c.tryStart().equals(label)) {
                            final Label target = c.handler();
                            if (!target.equals(label)) {
                                if (labelToIndex.containsKey(target)) {
                                    final int newIndex = labelToIndex.get(target);
                                    if (!visited.contains(newIndex)) {
                                        jobs.add(new CFGAnalysisJob(newIndex, newPath));
                                    }
                                    Frame frame = frames[newIndex];
                                    if (frame == null) {
                                        frame = new Frame(newIndex, codeElements.get(newIndex));
                                        frames[newIndex] = frame;
                                    }
                                    frame.predecessors.add(new CFGEdge(i, new NamedProjection("catch:" + exceptionIndex + ":" + (c.catchType().isPresent() ? c.catchType().get().asSymbol().descriptorString() : "any")), FlowType.FORWARD));
                                } else {
                                    illegalState("Exception handler target " + target + " is not mapped to an index");
                                }
                            }
                        }
                    }
                } else  if (current instanceof final Instruction instruction) {
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
                                FlowType flowType = FlowType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    flowType = FlowType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, NamedProjection.DEFAULT, flowType));
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
                                FlowType flowType = FlowType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    flowType = FlowType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, new NamedProjection("true"), flowType));
                            } else {
                                illegalState("Conditional branch to " + branch.target() + " which is not mapped to an index");
                            }
                        }
                    } else if (instruction instanceof final LookupSwitchInstruction lsu) {
                        final List<SwitchCase> cases = lsu.cases();
                        for (int j = 0; j < cases.size(); j++) {
                            final SwitchCase sc = cases.get(j);
                            final Label target = sc.target();

                            if (labelToIndex.containsKey(target)) {
                                final int newIndex = labelToIndex.get(target);
                                if (!visited.contains(newIndex)) {
                                    jobs.add(new CFGAnalysisJob(newIndex, newPath));
                                }
                                FlowType flowType = FlowType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    flowType = FlowType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, new NamedProjection("case" + j), flowType));
                            } else {
                                illegalState("Case branch to " + target + " which is not mapped to an index");
                            }
                        }

                        final Label target = lsu.defaultTarget();
                        if (labelToIndex.containsKey(target)) {
                            final int newIndex = labelToIndex.get(target);
                            if (!visited.contains(newIndex)) {
                                jobs.add(new CFGAnalysisJob(newIndex, newPath));
                            }
                            FlowType flowType = FlowType.FORWARD;
                            if (newPath.contains(newIndex)) {
                                flowType = FlowType.BACKWARD;
                            }
                            Frame frame = frames[newIndex];
                            if (frame == null) {
                                frame = new Frame(newIndex, codeElements.get(newIndex));
                                frames[newIndex] = frame;
                            }
                            frame.predecessors.add(new CFGEdge(i, new NamedProjection("default"), flowType));
                        } else {
                            illegalState("Default branch to " + target + " which is not mapped to an index");
                        }

                        break;
                    } else if (instruction instanceof final TableSwitchInstruction tsi) {
                        final List<SwitchCase> cases = tsi.cases();
                        for (int j = 0; j < cases.size(); j++) {
                            final SwitchCase sc = cases.get(j);
                            final Label target = sc.target();

                            if (labelToIndex.containsKey(target)) {
                                final int newIndex = labelToIndex.get(target);
                                if (!visited.contains(newIndex)) {
                                    jobs.add(new CFGAnalysisJob(newIndex, newPath));
                                }
                                FlowType flowType = FlowType.FORWARD;
                                if (newPath.contains(newIndex)) {
                                    flowType = FlowType.BACKWARD;
                                }
                                Frame frame = frames[newIndex];
                                if (frame == null) {
                                    frame = new Frame(newIndex, codeElements.get(newIndex));
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, new NamedProjection("case" + j), flowType));
                            } else {
                                illegalState("Case branch to " + target + " which is not mapped to an index");
                            }
                        }

                        final Label target = tsi.defaultTarget();
                        if (labelToIndex.containsKey(target)) {
                            final int newIndex = labelToIndex.get(target);
                            if (!visited.contains(newIndex)) {
                                jobs.add(new CFGAnalysisJob(newIndex, newPath));
                            }
                            FlowType flowType = FlowType.FORWARD;
                            if (newPath.contains(newIndex)) {
                                flowType = FlowType.BACKWARD;
                            }
                            Frame frame = frames[newIndex];
                            if (frame == null) {
                                frame = new Frame(newIndex, codeElements.get(newIndex));
                                frames[newIndex] = frame;
                            }
                            frame.predecessors.add(new CFGEdge(i, new NamedProjection("default"), flowType));
                        } else {
                            illegalState("Default branch to " + target + " which is not mapped to an index");
                        }

                        break;
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
                    nextFrame.predecessors.add(new CFGEdge(i, new NamedProjection("false"), FlowType.FORWARD));
                } else {
                    nextFrame.predecessors.add(new CFGEdge(i, NamedProjection.DEFAULT, FlowType.FORWARD));
                }

                if (visited.contains(i + 1)) {
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
        while (!currentPath.isEmpty()) {
            final Frame currentNode = currentPath.peek();
            final List<Frame> forwardNodes = new ArrayList<>();

            // This could be improved if we have to successor list directly...
            final Frame[] frames = getFrames();
            for (final Frame frame : frames) {
                // Maybe null due to unreachable statements in bytecode
                if (frame != null) {
                    for (final CFGEdge edge : frame.predecessors) {
                        if (edge.fromIndex == currentNode.elementIndex && edge.flowType == FlowType.FORWARD) {
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

        final Set<Label> exceptionHandlers = cm.exceptionHandlers().stream().map(ExceptionCatch::handler).collect(Collectors.toSet());

        // We need the topological (reverse-post-order) of the CFG to traverse
        // the instructions in the right order
        final List<Frame> topologicalOrder = getCodeModelTopologicalOrder();

        // Init locals according to the method signature and their types
        if (!topologicalOrder.getFirst().predecessors.isEmpty()) {
            // We have a cfg to the start, so the start should already be a loop header!
            final LoopHeaderNode loop = new LoopHeaderNode("Loop0");
            initStatus.control = initStatus.control.controlFlowsTo(loop, FlowType.FORWARD);

            // We start directly
            int localIndex = 0;
            if (!method.flags().flags().contains(AccessFlag.STATIC)) {
                final PHI p = loop.definePHI(owner);
                p.use(ir.defineThisRef(owner), new PHIUse(FlowType.FORWARD, ir));
                initStatus.setLocal(localIndex++, p);
            }
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                final PHI p = loop.definePHI(TypeUtils.jvmInternalTypeOf(argumentTypes[i]));
                p.use(ir.defineMethodArgument(TypeUtils.jvmInternalTypeOf(argumentTypes[i]), i), new PHIUse(FlowType.FORWARD, ir));
                initStatus.setLocal(localIndex++, p);
                if (TypeUtils.isCategory2(argumentTypes[i])) {
                    localIndex++;
                }
            }

        } else {
            // We start directly
            int localIndex = 0;
            if (!method.flags().flags().contains(AccessFlag.STATIC)) {
                initStatus.setLocal(localIndex++, ir.defineThisRef(owner));
            }
            final ClassDesc[] argumentTypes = methodTypeDesc.parameterArray();
            for (int i = 0; i < argumentTypes.length; i++) {
                initStatus.setLocal(localIndex++, ir.defineMethodArgument(TypeUtils.jvmInternalTypeOf(argumentTypes[i]), i));
                if (TypeUtils.isCategory2(argumentTypes[i])) {
                    localIndex++;
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

            boolean isExceptionHandler = false;
            final CodeElement frameElement = cm.elementList().get(frame.elementIndex);
            if (frameElement instanceof final LabelTarget labelTarget) {
                // Check is this is an exception handler
                isExceptionHandler = exceptionHandlers.contains(labelTarget.label());
            }

            Status incomingStatus = frame.in;
            if (incomingStatus == null) {
                // We need to compute the incoming status from the predecessors
                if (frame.predecessors.isEmpty()) {
                    illegalState("No predecessors for " + frame.elementIndex);
                }
                if (frame.predecessors.size() == 1) {
                    final CFGEdge edge = frame.predecessors.getFirst();
                    final Frame outgoing = posToFrame.get(edge.fromIndex);
                    if (outgoing == null) {
                        illegalState("No outgoing frame for " + frame.elementIndex);
                    }
                    if (outgoing.out == null) {
                        illegalState("No outgoing status for " + frame.elementIndex);
                    }
                    incomingStatus = outgoing.out.copy();
                    // TODO: In case of exception handlers we do not have a guaranteed stack, but only the provided exception

                    if (incomingStatus.control instanceof TupleNode) {
                        incomingStatus.control = ((TupleNode) incomingStatus.control).getNamedNode(edge.projection().name());
                    } else if (NamedProjection.DEFAULT.equals(edge.projection)) {
                        // No nothing in this case, we just keep the incoming control node
                    } else {
                        illegalState("Unknown projection type " + edge.projection + " or unsupported node : " + incomingStatus.control);
                    }

                    if (isExceptionHandler) {
                        incomingStatus.stack.clear();
                        if (!(incomingStatus.control instanceof Catch)) {
                            illegalState("Catch node expected for exception handler, not " + incomingStatus.control);
                        }

                        // We put the caught exception on the stack here
                        final Catch c = (Catch) incomingStatus.control;
                        incomingStatus.stack.push(c.caughtException());
                    }

                    frame.in = incomingStatus;
                } else {

                    if (isExceptionHandler) {
                        illegalState("Exception handler with multiple predecessors not supported yet");
                    }

                    // Eager PHI creation and memory-edge merging
                    final List<Frame> incomingFrames = new ArrayList<>();
                    final List<Node> incomingMemories = new ArrayList<>();
                    boolean hasBackEdges = false;
                    for (final CFGEdge edge : frame.predecessors) {
                        // We only respect forward control flows, as phi data propagation for backward
                        // edges is handled during node parsing of the source instruction, as
                        // the outgoing status for this node is not computed yet.
                        if (edge.flowType == FlowType.FORWARD) {
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
                        target = new LoopHeaderNode("Loop" + frame.elementIndex);
                    } else {
                        target = new MergeNode("Merge" + frame.elementIndex);
                    }

                    for (final CFGEdge edge : frame.predecessors) {
                        // We only respect forward control flows, as phi data propagation for backward
                        // edges is handled during node parsing of the source instruction, as
                        // the outgoing status for this node is not computed yet.
                        if (edge.flowType == FlowType.FORWARD) {
                            final Frame incomingFrame = posToFrame.get(edge.fromIndex);
                            incomingFrame.out.control.controlFlowsTo(target, FlowType.FORWARD);
                        }
                    }

                    incomingStatus = new Status(cm.maxLocals());

                    // TODO: In case of exception handlers we do not have a guaranteed stack, but only the provided exception

                    int incomingStackSize = -1;
                    for (final Frame fr : incomingFrames) {
                        if (incomingStackSize == -1) {
                            incomingStackSize = fr.out.stack.size();
                        } else if (incomingStackSize != fr.out.stack.size()) {
                            illegalState("Stack size mismatch for frame at " + frame.elementIndex + " expected " + incomingStackSize + " but got " + fr.out.stack.size());
                        }
                    }

                    if (incomingStackSize > 0) {
                        // Check of we need to do something
                        for (int stackPos = 0; stackPos < incomingStackSize; stackPos++) {
                            final List<Value> allValues = new ArrayList<>();
                            for (final Frame fr : incomingFrames) {
                                final Value sv = fr.out.stack.get(stackPos);
                                if (sv != null) {
                                    if (!allValues.contains(sv)) {
                                        allValues.add(sv);
                                    }
                                }
                            }
                            if (allValues.size() > 1 || hasBackEdges) {
                                final Value source = allValues.getFirst();
                                final PHI p = target.definePHI(source.type);

                                for (final Frame fr : incomingFrames) {
                                    final Value sv = fr.out.stack.get(stackPos);
                                    p.use(sv, new PHIUse(FlowType.FORWARD, fr.out.control));
                                }

                                incomingStatus.stack.push(p);
                          } else {
                                incomingStatus.stack.push(allValues.getFirst());
                            }
                        }
                    }

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
                                    p.use(sv, new PHIUse(FlowType.FORWARD, incomingFrame.out.control));
                                }
                                incomingStatus.setLocal(mergeLocalIndex, p);
                            } else {
                                incomingStatus.setLocal(mergeLocalIndex, allValues.getFirst());
                            }
                        }
                    }

                    if (frame.codeElement instanceof final LabelTarget lt) {
                        final List<ExceptionCatch> catchesEndingHere = code.exceptionHandlers().stream().filter(t -> t.tryEnd().equals(lt.label())).toList();
                        if (!catchesEndingHere.isEmpty()) {
                            illegalState("Merge at end of exception handler not supported yet");
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
                }
            }

            if (frame.entryPoint == null) {
                // This is the thing we need to interpret

                // Interpret the node
                visitNode(code, frameElement, frame);

                if (frame.out == null || frame.out == incomingStatus) {
                    illegalState("No outgoing or same same as incoming status for " + frameElement);
                }
            } else {
                frame.copyIncomingToOutgoing();
            }
        }
    }

    private void visitNode(final CodeModel codeModel, final CodeElement node, final Frame frame) {

        if (node instanceof final PseudoInstruction psi) {
            // Pseudo Instructions
            switch (psi) {
                case final LabelTarget labelTarget -> visitLabelTarget(codeModel, labelTarget, frame);
                case final LineNumber lineNumber -> visitLineNumberNode(lineNumber, frame);
                case final LocalVariable localVariable ->
                    // Maybe we can use this for debugging?
                        frame.copyIncomingToOutgoing();
                case final LocalVariableType localVariableType -> frame.copyIncomingToOutgoing();
                case final CharacterRange characterRange -> frame.copyIncomingToOutgoing();
                case final ExceptionCatch exceptionCatch -> visitExceptionCatch(exceptionCatch, frame);
                default -> throw new IllegalArgumentException("Not implemented yet : " + psi);
            }
        } else if (node instanceof final Instruction ins) {
            // Real bytecode instructions
            switch (ins) {
                case final IncrementInstruction incrementInstruction ->
                        parse_IINC(incrementInstruction.slot(), incrementInstruction.constant(), frame);
                case final InvokeInstruction invokeInstruction ->
                        visitInvokeInstruction(invokeInstruction.opcode(), invokeInstruction.owner().asSymbol(), invokeInstruction.name().stringValue(), invokeInstruction.typeSymbol(), frame);
                case final LoadInstruction load -> visitLoadInstruction(load.opcode(), load.slot(), frame);
                case final StoreInstruction store -> visitStoreInstruction(store.opcode(), store.slot(), frame);
                case final BranchInstruction branchInstruction -> visitBranchInstruction(branchInstruction, frame);
                case final ConstantInstruction constantInstruction ->
                        visitConstantInstruction(constantInstruction.opcode(), constantInstruction.constantValue(), frame);
                case final FieldInstruction fieldInstruction ->
                        visitFieldInstruction(fieldInstruction.opcode(), fieldInstruction.field().owner().asSymbol(), fieldInstruction.typeSymbol(), fieldInstruction.name().stringValue(), frame);
                case final NewObjectInstruction newObjectInstruction ->
                        visitNewObjectInstruction(newObjectInstruction.opcode(), newObjectInstruction.className().asSymbol(), frame);
                case final ReturnInstruction returnInstruction ->
                        visitReturnInstruction(returnInstruction.opcode(), frame);
                case final InvokeDynamicInstruction invokeDynamicInstruction ->
                        parse_INVOKEDYNAMIC(invokeDynamicInstruction, frame);
                case final TypeCheckInstruction typeCheckInstruction ->
                        visitTypeCheckInstruction(typeCheckInstruction.opcode(), typeCheckInstruction.type().asSymbol(), frame);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction.opcode(), frame);
                case final OperatorInstruction operatorInstruction ->
                        visitOperatorInstruction(operatorInstruction.opcode(), frame);
                case final MonitorInstruction monitorInstruction ->
                        visitMonitorInstruction(monitorInstruction.opcode(), frame);
                case final ThrowInstruction thr -> visitThrowInstruction(frame);
                case final NewPrimitiveArrayInstruction na -> visitNewPrimitiveArray(na.typeKind(), frame);
                case final ArrayStoreInstruction as -> visitArrayStoreInstruction(as.opcode(), frame);
                case final ArrayLoadInstruction al -> visitArrayLoadInstruction(al.opcode(), frame);
                case final NopInstruction nop -> visitNopInstruction(frame);
                case final NewReferenceArrayInstruction rei ->
                        visitNewObjectArray(rei.componentType().asSymbol(), frame);
                case final ConvertInstruction ci -> visitConvertInstruction(ci.opcode(), frame);
                case final NewMultiArrayInstruction nm ->
                        visitNewMultiArray(nm.arrayType().asSymbol(), nm.dimensions(), frame);
                case final LookupSwitchInstruction lsi ->
                        visitLookupSwitchInstruction(lsi.cases(), lsi.defaultTarget().toString(), frame);
                case final TableSwitchInstruction tsi ->
                        visitTableSwitchInstruction(tsi.lowValue(), tsi.highValue(), tsi.cases(), tsi.defaultTarget().toString(), frame);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    protected void visitLookupSwitchInstruction(final List<SwitchCase> switchCases, final String defaultLabel, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value check = outgoing.pop();

        // TODO: Back edges not allowed here!

        final List<Integer> cases = new ArrayList<>();
        for (final SwitchCase sc : switchCases) {
            cases.add(sc.caseValue());
        }

        final Node node = new LookupSwitch(check, defaultLabel, cases);
        outgoing.control = outgoing.control.controlFlowsTo(node, FlowType.FORWARD);
    }

    protected void visitTableSwitchInstruction(final int minValue, final int maxValue, final List<SwitchCase> switchCases, final String defaultLabel, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value check = outgoing.pop();

        // TODO: Back edges not allowed here!

        final List<Integer> cases = new ArrayList<>();
        for (final SwitchCase sc : switchCases) {
            cases.add(sc.caseValue());
        }

        final Node node = new TableSwitch(check, minValue, maxValue, defaultLabel, cases);
        outgoing.control = outgoing.control.controlFlowsTo(node, FlowType.FORWARD);
    }

    @Testbacklog
    protected void visitLabelTarget(final CodeModel codeModel, final LabelTarget node, final Frame frame) {

        final Label label = node.label();
        final List<ExceptionCatch> catchesFromHere = codeModel.exceptionHandlers().stream().filter(t -> t.tryStart().equals(label)).toList();
        final Map<Label, List<ExceptionCatch>> catchesEndingHere = codeModel.exceptionHandlers().stream().filter(t -> t.tryEnd().equals(label)).collect(Collectors.groupingBy(ExceptionCatch::tryStart));

        if (!catchesEndingHere.isEmpty()) {
            if (!catchesFromHere.isEmpty()) {
                illegalState("Not implemented yet here: Label starts and ends an TryCatch at the same time");
            }
            if (catchesEndingHere.size() > 1) {
                illegalState("Multiple TryCatch ending at the same label not implemented yet");
            }
            final Status outgoing = frame.copyIncomingToOutgoing();
            final ExceptionGuard activeGuard = outgoing.popExceptionGuard();

            final Node n = new MergeNode("EndOfGuardedBlock" + frame.elementIndex);
            outgoing.control.controlFlowsTo(n, FlowType.FORWARD);
            outgoing.control = activeGuard.exitNode().controlFlowsTo(n, FlowType.FORWARD);

            frame.entryPoint = n;
            return;
        }

        if (catchesFromHere.isEmpty()) {
            final Status outgoing = frame.copyIncomingToOutgoing();
            final Node n = new LabelNode("Frame" + frame.elementIndex);
            outgoing.control = outgoing.control.controlFlowsTo(n, FlowType.FORWARD);

            frame.entryPoint = n;
        } else {
            final Status outgoing = frame.copyIncomingToOutgoing();
            final ExceptionGuard n = new ExceptionGuard(catchesFromHere.stream().map(t -> {
                if (t.catchType().isPresent()) {
                    return new ExceptionGuard.Catches(Optional.of(t.catchType().get().asSymbol()));
                }
                return new ExceptionGuard.Catches(Optional.empty());
            }).toList());
            outgoing.registerExceptionGuard(n);
            outgoing.control = outgoing.control.controlFlowsTo(n, FlowType.FORWARD);

            frame.entryPoint = n;
        }
    }

    @Testbacklog
    protected void visitLineNumberNode(final LineNumber node, final Frame frame) {
        // A node that represents a line number declaration. These nodes are pseudo-instruction nodes inorder to be inserted in an instruction list.
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.lineNumber = node.line();
    }

    private void visitExceptionCatch(final ExceptionCatch node, final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    protected void parse_IINC(final int slot, final int constant, final Frame frame) {
        // A node that represents an IINC instruction.
        final Status outgoing = frame.copyIncomingToOutgoing();
        final Value value = frame.in.getLocal(slot);
        if (value == null) {
            illegalState("No local value for slot " + slot);
        }
        frame.out.setLocal(slot, new Add(ConstantDescs.CD_int, value, outgoing.control.definePrimitiveInt(constant)));
    }

    protected void visitInvokeInstruction(final Opcode opcode, final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc, final Frame frame) {
        // A node that represents a method instruction. A method instruction is an instruction that invokes a method.
        switch (opcode) {
            case Opcode.INVOKESPECIAL -> parse_INVOKESPECIAL(owner, methodName, methodTypeDesc, frame);
            case Opcode.INVOKEVIRTUAL -> parse_INVOKEVIRTUAL(owner, methodName, methodTypeDesc, frame);
            case Opcode.INVOKEINTERFACE -> parse_INVOKEINTERFACE(owner, methodName, methodTypeDesc, frame);
            case Opcode.INVOKESTATIC -> parse_INVOKESTATIC(owner, methodName, methodTypeDesc, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitLoadInstruction(final Opcode opcode, final int slot, final Frame frame) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (opcode) {
            case Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_2, Opcode.ALOAD_1, Opcode.ALOAD_0, Opcode.ALOAD_W ->
                    parse_ALOAD(slot, frame);
            case Opcode.ILOAD, Opcode.ILOAD_3, Opcode.ILOAD_2, Opcode.ILOAD_1, Opcode.ILOAD_0, Opcode.ILOAD_W ->
                    parse_LOAD_TYPE(slot, frame, ConstantDescs.CD_int);
            case Opcode.DLOAD, Opcode.DLOAD_3, Opcode.DLOAD_2, Opcode.DLOAD_1, Opcode.DLOAD_0, Opcode.DLOAD_W ->
                    parse_LOAD_TYPE(slot, frame, ConstantDescs.CD_double);
            case Opcode.FLOAD, Opcode.FLOAD_3, Opcode.FLOAD_2, Opcode.FLOAD_1, Opcode.FLOAD_0, Opcode.FLOAD_W ->
                    parse_LOAD_TYPE(slot, frame, ConstantDescs.CD_float);
            case Opcode.LLOAD, Opcode.LLOAD_3, Opcode.LLOAD_2, Opcode.LLOAD_1, Opcode.LLOAD_0, Opcode.LLOAD_W ->
                    parse_LOAD_TYPE(slot, frame, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitStoreInstruction(final Opcode opcode, final int slot, final Frame frame) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (opcode) {
            case Opcode.ASTORE, Opcode.ASTORE_3, Opcode.ASTORE_2, Opcode.ASTORE_1, Opcode.ASTORE_0, Opcode.ASTORE_W ->
                    parse_ASTORE(slot, frame);
            case Opcode.ISTORE, Opcode.ISTORE_3, Opcode.ISTORE_2, Opcode.ISTORE_1, Opcode.ISTORE_0, Opcode.ISTORE_W ->
                    parse_STORE_TYPE(slot, frame, ConstantDescs.CD_int);
            case Opcode.LSTORE, Opcode.LSTORE_3, Opcode.LSTORE_2, Opcode.LSTORE_1, Opcode.LSTORE_0, Opcode.LSTORE_W ->
                    parse_STORE_TYPE(slot, frame, ConstantDescs.CD_long);
            case Opcode.FSTORE, Opcode.FSTORE_0, Opcode.FSTORE_1, Opcode.FSTORE_2, Opcode.FSTORE_3, Opcode.FSTORE_W ->
                    parse_STORE_TYPE(slot, frame, ConstantDescs.CD_float);
            case Opcode.DSTORE, Opcode.DSTORE_0, Opcode.DSTORE_1, Opcode.DSTORE_2, Opcode.DSTORE_3, Opcode.DSTORE_W ->
                    parse_STORE_TYPE(slot, frame, ConstantDescs.CD_double);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
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

    protected void visitFieldInstruction(final Opcode opcode, final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Frame frame) {
        switch (opcode) {
            case GETFIELD -> parse_GETFIELD(owner, fieldType, fieldName, frame);
            case PUTFIELD -> parse_PUTFIELD(owner, fieldType, fieldName, frame);
            case GETSTATIC -> parse_GETSTATIC(owner, fieldType, fieldName, frame);
            case PUTSTATIC -> parse_PUTSTATIC(owner, fieldType, fieldName, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitNewObjectInstruction(final Opcode opcode, final ClassDesc type, final Frame frame) {
        switch (opcode) {
            case Opcode.NEW -> parse_NEW(type, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
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

        // We need the bootstrap method as the later invocation target
        final DirectMethodHandleDesc bootstrapMethod = node.bootstrapMethod();
        if (bootstrapMethod.kind() != DirectMethodHandleDesc.Kind.STATIC) {
            illegalState("Only static bootstrap methods are supported yet");
        }
        final List<Value> bootstrapArguments = new ArrayList<>();

        final Value invokerType = ir.defineRuntimeclassReference(owner);
        final ClassDesc lookupOwner = ClassDesc.of(MethodHandles.Lookup.class.getName());

        // Default bootstrap arguments
        bootstrapArguments.add(new InvokeStatic(lookupOwner, invokerType, "in", MethodTypeDesc.of(lookupOwner, ClassDesc.of(Class.class.getName())), List.of(invokerType)));
        bootstrapArguments.add(outgoing.control.defineStringConstant(node.name().stringValue()));
        bootstrapArguments.add(constantToValue(outgoing.control, node.typeSymbol()));

        final MethodTypeDesc bootstrapMethodType = bootstrapMethod.invocationType();

        // TODO: Check arity...
        for (final ConstantDesc bootstrapArgument : node.bootstrapArgs()) {
            bootstrapArguments.add(constantToValue(outgoing.control, bootstrapArgument));
        }
        if (bootstrapArguments.size() < bootstrapMethodType.parameterCount()) {
            if (bootstrapMethodType.parameterType(bootstrapMethodType.parameterCount() - 1).isArray()) {
                // TODO: Check for memory flow...
                final NewArray emptyArgs = new NewArray(ConstantDescs.CD_Object, outgoing.control.definePrimitiveInt(0));
                bootstrapArguments.add(emptyArgs);
            } else {
                illegalState("Don't get this signature for invokedynamic here...");
            }
        }
        final Value bootstrapInvocation = new InvokeStatic(bootstrapMethod.owner(), ir.defineRuntimeclassReference(bootstrapMethod.owner()), bootstrapMethod.methodName(), bootstrapMethodType, bootstrapArguments);

        final List<Value> dynamicArguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            dynamicArguments.add(frame.out.pop());
        }

        final Value invokeDynamic = new InvokeDynamic(owner, bootstrapInvocation, node.name().stringValue(), methodTypeDesc, dynamicArguments.reversed());

        outgoing.memory = outgoing.memory.memoryFlowsTo(invokeDynamic);
        outgoing.control = outgoing.control.controlFlowsTo(invokeDynamic, FlowType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            frame.out.push(invokeDynamic);
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

    protected void visitStackInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.DUP -> parse_DUP(frame);
            case Opcode.DUP_X1 -> parse_DUP_X1(frame);
            case Opcode.DUP_X2 -> parse_DUP_X2(frame);
            case Opcode.DUP2 -> parse_DUP2(frame);
            case Opcode.DUP2_X1 -> parse_DUP2_X1(frame);
            case Opcode.DUP2_X2 -> parse_DUP2_X2(frame);
            case Opcode.POP -> parse_POP(frame);
            case Opcode.POP2 -> parse_POP2(frame);
            case Opcode.SWAP -> parse_SWAP(frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitOperatorInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.IADD, Opcode.DADD, Opcode.FADD, Opcode.LADD, Opcode.FSUB, Opcode.ISUB, Opcode.DSUB,
                 Opcode.LSUB, Opcode.DMUL, Opcode.FMUL, Opcode.IMUL, Opcode.LMUL, Opcode.IDIV, Opcode.LDIV,
                 Opcode.FDIV, Opcode.DDIV, Opcode.LREM, Opcode.IREM, Opcode.FREM, Opcode.DREM, Opcode.IAND,
                 Opcode.IOR, Opcode.IXOR, Opcode.ISHL, Opcode.ISHR, Opcode.IUSHR, Opcode.LAND, Opcode.LOR,
                 Opcode.LXOR, Opcode.LSHL, Opcode.LSHR, Opcode.LUSHR, Opcode.FCMPG, Opcode.FCMPL, Opcode.DCMPG,
                 Opcode.DCMPL, Opcode.LCMP -> visitBinaryOperatorInstruction(opcode, frame);
            case Opcode.ARRAYLENGTH, Opcode.INEG, Opcode.LNEG, Opcode.FNEG, Opcode.DNEG ->
                    visitUnaryOperatorInstruction(opcode, frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    private void visitBinaryOperatorInstruction(final Opcode opcode, final Frame frame) {
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
            case Opcode.DMUL -> parse_MUL_X(frame, value1, value2, ConstantDescs.CD_double);
            case Opcode.FMUL -> parse_MUL_X(frame, value1, value2, ConstantDescs.CD_float);
            case Opcode.IMUL -> parse_MUL_X(frame, value1, value2, ConstantDescs.CD_int);
            case Opcode.LMUL -> parse_MUL_X(frame, value1, value2, ConstantDescs.CD_long);
            case Opcode.IDIV -> parse_DIV_X(frame, value1, value2, ConstantDescs.CD_int);
            case Opcode.LDIV -> parse_DIV_X(frame, value1, value2, ConstantDescs.CD_long);
            case Opcode.FDIV -> parse_DIV_X(frame, value1, value2, ConstantDescs.CD_float);
            case Opcode.DDIV -> parse_DIV_X(frame, value1, value2, ConstantDescs.CD_double);
            case Opcode.IREM -> parse_REM_X(frame, value1, value2, ConstantDescs.CD_int);
            case Opcode.LREM -> parse_REM_X(frame, value1, value2, ConstantDescs.CD_long);
            case Opcode.FREM -> parse_REM_X(frame, value1, value2, ConstantDescs.CD_float);
            case Opcode.DREM -> parse_REM_X(frame, value1, value2, ConstantDescs.CD_double);
            case Opcode.IAND ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.AND);
            case Opcode.IOR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.OR);
            case Opcode.IXOR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.XOR);
            case Opcode.ISHL ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.SHL);
            case Opcode.ISHR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.SHR);
            case Opcode.IUSHR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_int, BitOperation.Operation.USHR);
            case Opcode.LAND ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.AND);
            case Opcode.LOR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.OR);
            case Opcode.LXOR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.XOR);
            case Opcode.LSHL ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.SHL);
            case Opcode.LSHR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.SHR);
            case Opcode.LUSHR ->
                    parse_BITOPERATION_X(frame, value1, value2, ConstantDescs.CD_long, BitOperation.Operation.USHR);
            case Opcode.FCMPG ->
                    parse_NUMERICCOMPARE_X(frame, value1, value2, ConstantDescs.CD_float, NumericCompare.Mode.NAN_IS_1);
            case Opcode.FCMPL ->
                    parse_NUMERICCOMPARE_X(frame, value1, value2, ConstantDescs.CD_float, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.DCMPG ->
                    parse_NUMERICCOMPARE_X(frame, value1, value2, ConstantDescs.CD_double, NumericCompare.Mode.NAN_IS_1);
            case Opcode.DCMPL ->
                    parse_NUMERICCOMPARE_X(frame, value1, value2, ConstantDescs.CD_double, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.LCMP ->
                    parse_NUMERICCOMPARE_X(frame, value1, value2, ConstantDescs.CD_long, NumericCompare.Mode.NONFLOATINGPOINT);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    private void visitUnaryOperatorInstruction(final Opcode opcode, final Frame frame) {
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

    protected void visitMonitorInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.MONITORENTER -> parse_MONITORENTER(frame);
            case Opcode.MONITOREXIT -> parse_MONITOREXIT(frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitThrowInstruction(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        final Throw t = new Throw(v);
        outgoing.control = outgoing.control.controlFlowsTo(t, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(t);
        frame.entryPoint = t;
    }

    protected void visitNewPrimitiveArray(final TypeKind kind, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value length = frame.out.pop();
        final ClassDesc type;
        switch (kind) {
            case BYTE -> type = ConstantDescs.CD_byte.arrayType();
            case SHORT -> type = ConstantDescs.CD_short.arrayType();
            case BOOLEAN -> type = ConstantDescs.CD_boolean.arrayType();
            case CHAR -> type = ConstantDescs.CD_char.arrayType();
            case INT -> type = ConstantDescs.CD_int.arrayType();
            case LONG -> type = ConstantDescs.CD_long.arrayType();
            case FLOAT -> type = ConstantDescs.CD_float.arrayType();
            case DOUBLE -> type = ConstantDescs.CD_double.arrayType();
            default -> throw new IllegalArgumentException("Not implemented type kind for array creation " + kind);
        }
        final NewArray newArray = new NewArray(type.componentType(), length);
        frame.out.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        frame.entryPoint = newArray;
    }

    protected void visitArrayStoreInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.BASTORE -> parse_ARRAYSTORE_X_TRUNCATED(opcode, frame);
            case Opcode.CASTORE -> parse_ARRAYSTORE_X_TRUNCATED(opcode, frame);
            case Opcode.SASTORE -> parse_ARRAYSTORE_X_TRUNCATED(opcode, frame);
            case Opcode.IASTORE -> parse_ASTORE_X(frame, ConstantDescs.CD_int.arrayType());
            case Opcode.LASTORE -> parse_ASTORE_X(frame, ConstantDescs.CD_long.arrayType());
            case Opcode.FASTORE -> parse_ASTORE_X(frame, ConstantDescs.CD_float.arrayType());
            case Opcode.DASTORE -> parse_ASTORE_X(frame, ConstantDescs.CD_double.arrayType());
            case Opcode.AASTORE -> parse_AASTORE(frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitArrayLoadInstruction(final Opcode opcode, final Frame frame) {
        switch (opcode) {
            case Opcode.BALOAD ->
                    parse_ALOAD_X_INTEXTENDED(frame, Extend.ExtendType.SIGN); // Sign extend!
            case Opcode.CALOAD ->
                    parse_ALOAD_X_INTEXTENDED(frame, Extend.ExtendType.ZERO); // Zero extend!
            case Opcode.SALOAD ->
                    parse_ALOAD_X_INTEXTENDED(frame, Extend.ExtendType.SIGN); // Sign extend!
            case Opcode.IALOAD -> parse_ALOAD_X(frame, ConstantDescs.CD_int.arrayType());
            case Opcode.LALOAD -> parse_ALOAD_X(frame, ConstantDescs.CD_long.arrayType());
            case Opcode.FALOAD -> parse_ALOAD_X(frame, ConstantDescs.CD_float.arrayType());
            case Opcode.DALOAD ->
                    parse_ALOAD_X(frame, ConstantDescs.CD_double.arrayType());
            case Opcode.AALOAD -> parse_AALOAD(frame);
            default -> throw new IllegalArgumentException("Not implemented yet : " + opcode);
        }
    }

    protected void visitNopInstruction(final Frame frame) {
        frame.copyIncomingToOutgoing();
    }

    protected void visitNewObjectArray(final ClassDesc componentType, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final NewArray newArray = new NewArray(componentType, outgoing.pop());
        outgoing.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        frame.entryPoint = newArray;
    }

    private void parse_truncate_and_extend(final Opcode opcode, final Frame frame, final ClassDesc targetType, final ClassDesc truncatedTo, final Extend.ExtendType extendType) {
        assertMinimumStackSize(frame.in, 1);

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value value = frame.out.pop();
        if (!ConstantDescs.CD_int.equals(value.type)) {
            illegalState("Expected an int on stack for " + opcode + ", but got a " + TypeUtils.toString(value.type));
        }

        outgoing.stack.push(new Extend(targetType, extendType, new Truncate(truncatedTo, value)));
    }

    private void parse_extend(final Opcode opcode, final Frame frame, final ClassDesc expectedType, final ClassDesc targetType, final Extend.ExtendType extendType) {
        assertMinimumStackSize(frame.in, 1);

        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value value = frame.out.pop();
        if (!expectedType.equals(value.type)) {
            illegalState("Expected an " + TypeUtils.toString(expectedType) + " on stack for " + opcode + ", but got a " + TypeUtils.toString(value.type));
        }

        outgoing.stack.push(new Extend(targetType, extendType, value));
    }

    protected void visitConvertInstruction(final Opcode opcode, final Frame frame) {
        assertMinimumStackSize(frame.in, 1);

        switch (opcode) {
            case Opcode.I2B -> parse_truncate_and_extend(opcode, frame, ConstantDescs.CD_int, ConstantDescs.CD_byte, Extend.ExtendType.SIGN);
            case Opcode.I2C -> parse_truncate_and_extend(opcode, frame, ConstantDescs.CD_int, ConstantDescs.CD_char, Extend.ExtendType.ZERO);
            case Opcode.I2S -> parse_truncate_and_extend(opcode, frame, ConstantDescs.CD_int, ConstantDescs.CD_short, Extend.ExtendType.SIGN);
            // TODO: Extend cases, maybe remove convert instruction?
            case Opcode.I2L -> parse_extend(opcode, frame, ConstantDescs.CD_int, ConstantDescs.CD_long, Extend.ExtendType.SIGN);
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

    protected void visitNewMultiArray(final ClassDesc arrayType, final int dimensionSize, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, dimensionSize);

        final List<Value> dimensions = new ArrayList<>();
        for (int i = 0; i < dimensionSize; i++) {
            dimensions.add(outgoing.stack.pop());
        }
        final NewMultiArray newMultiArray = new NewMultiArray(arrayType, dimensions);
        outgoing.push(newMultiArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newMultiArray);
        frame.entryPoint = newMultiArray;
    }

    private void parse_INVOKESPECIAL(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc, final Frame frame) {

        final Status outgoing = frame.copyIncomingToOutgoing();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();

        assertMinimumStackSize(outgoing, args.length + 1);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            arguments.add(outgoing.pop());
        }

        final Value target = outgoing.pop();

        final Value next = new InvokeSpecial(owner, target, methodName, methodTypeDesc, arguments.reversed());
        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(next);
        }

        frame.entryPoint = next;
    }

    private void parse_INVOKEVIRTUAL(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc, final Frame frame) {

        final Status outgoing = frame.copyIncomingToOutgoing();

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();

        assertMinimumStackSize(outgoing, args.length + 1);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }

        final Value target = outgoing.pop();

        final Invoke invoke = new InvokeVirtual(owner, target, methodName, methodTypeDesc, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invoke, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invoke);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invoke);
        }

        frame.entryPoint = invoke;
    }

    private void parse_INVOKEINTERFACE(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc, final Frame frame) {

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, args.length + 1);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }

        final Value target = outgoing.pop();

        final Invoke invoke = new InvokeInterface(owner, target, methodName, methodTypeDesc, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invoke, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invoke);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invoke);
        }

        frame.entryPoint = invoke;
    }

    private void parse_INVOKESTATIC(final ClassDesc owner, final String methodName, final MethodTypeDesc methodTypeDesc, final Frame frame) {

        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, args.length);

        final RuntimeclassReference runtimeClass = ir.defineRuntimeclassReference(owner);
        final ClassInitialization init = new ClassInitialization(runtimeClass);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            final Value v = outgoing.pop();
            arguments.add(v);
        }

        final Invoke invoke = new InvokeStatic(owner, init, methodName, methodTypeDesc, arguments.reversed());

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invoke);
        outgoing.control = outgoing.control.controlFlowsTo(init, FlowType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(invoke, FlowType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.push(invoke);
        }

        frame.entryPoint = init;
    }

    private void parse_ALOAD(final int slot, final Frame frame) {
        final Value v = frame.in.getLocal(slot);
        if (v == null) {
            illegalState("Slot " + slot + " is null");
        }
        if (v.isPrimitive()) {
            illegalState("Cannot load primitive value " + TypeUtils.toString(v.type) + " for slot " + slot);
        }

        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(v);
    }

    private void parse_LOAD_TYPE(final int slot, final Frame frame, final ClassDesc type) {
        final Value v = frame.in.getLocal(slot);
        if (v == null) {
            illegalState("Slot " + slot + " is null");
        }
        if (!v.type.equals(type)) {
            illegalState("Cannot load " + TypeUtils.toString(v.type) + " from slot " + slot + " as " + TypeUtils.toString(type) + " is expected!");
        }
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(v);
    }

    private void parse_ASTORE(final int slot, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (v.isPrimitive()) {
            illegalState("Cannot store primitive value " + TypeUtils.toString(v.type));
        }

        frame.out.setLocal(slot, v);
    }

    private void parse_STORE_TYPE(final int slot, final Frame frame, final ClassDesc type) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        if (!v.type.equals(type)) {
            illegalState("Cannot store non " + TypeUtils.toString(type) + " value " + TypeUtils.toString(v.type) + " for slot " + slot);
        }
        frame.out.setLocal(slot, v);
    }

    @Testbacklog
    protected void parse_IF_ICMP_OP(final BranchInstruction node, final Frame frame, final NumericCondition.Operation op) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v1, v2);
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
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

        final NumericCondition numericCondition = new NumericCondition(op, v, outgoing.control.definePrimitiveInt(0));
        final If next = new If(numericCondition);

        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
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

        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
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

        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
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
                    target.use(v, new PHIUse(FlowType.BACKWARD, jumpSource));
                }
            }

            if (!outgoing.stack.isEmpty()) {
                illegalState("Don't know how to handle a back edge with a non empty stack!");
            }

            outgoing.control = outgoing.control.controlFlowsTo(targetFrame.entryPoint, FlowType.BACKWARD);
        }
    }

    @Testbacklog
    protected void parse_GOTO(final BranchInstruction node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Goto next = new Goto();
        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
        frame.entryPoint = next;

        final int codeElementIndex = labelToIndex.get(node.target());
        final Frame targetFrame = frames[codeElementIndex];

        if (targetFrame.indexInTopologicalOrder == -1) {
            illegalState("Cannot jump to unvisited frame at index " + targetFrame.elementIndex);
        }

        handlePotentialBackedgeFor(next, frame, targetFrame);
    }

    private Value constantToValue(final Node control, final ConstantDesc constantDesc) {
        return switch (constantDesc) {
            case final String str -> control.defineStringConstant(str);
            case final Integer i -> control.definePrimitiveInt(i);
            case final Long l -> control.definePrimitiveLong(l);
            case final Float f -> control.definePrimitiveFloat(f);
            case final Double d -> control.definePrimitiveDouble(d);
            case final ClassDesc classDesc -> ir.defineRuntimeclassReference(classDesc);
            case final MethodTypeDesc mtd -> ir.defineMethodType(mtd);
            case final MethodHandleDesc mh -> ir.defineMethodHandle(mh);
            case null, default -> {
                illegalState("Cannot convert " + constantDesc + " to IR value");
                yield null;
            }
        };
    }

    protected void parse_LDC(final ConstantDesc value, final Frame frame) {
        // A node that represents an LDC instruction.
        final Status outgoing = frame.copyIncomingToOutgoing();

        outgoing.push(constantToValue(outgoing.control, value));
    }

    private void parse_ICONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveInt((Integer) node));
    }

    private void parse_LCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveLong((Long) node));
    }

    private void parse_FCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveFloat((Float) node));
    }

    private void parse_DCONST(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveDouble((Double) node));
    }

    private void parse_BIPUSH(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveInt((Integer) node));
    }

    private void parse_SIPUSH(final ConstantDesc node, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.definePrimitiveInt(((Number) node).shortValue()));
    }

    private void parse_ACONST_NULL(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        outgoing.push(outgoing.control.defineNullReference());
    }

    private void parse_GETFIELD(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        final GetField get = new GetField(owner, fieldType, fieldName, v);
        outgoing.push(get);

        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    private void parse_PUTFIELD(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v = outgoing.pop();
        final Value target = outgoing.pop();

        final PutField put = new PutField(owner, fieldType, fieldName, target, v);

        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(put, FlowType.FORWARD);
    }

    private void parse_GETSTATIC(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(owner);
        final ClassInitialization init = new ClassInitialization(ri);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final GetStatic get = new GetStatic(ri, fieldName, fieldType);
        outgoing.push(get);

        outgoing.control = outgoing.control.controlFlowsTo(init, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
    }

    private void parse_PUTSTATIC(final ClassDesc owner, final ClassDesc fieldType, final String fieldName, final Frame frame) {
        final RuntimeclassReference ri = ir.defineRuntimeclassReference(owner);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final Value v = outgoing.pop();

        final PutStatic put = new PutStatic(ri, fieldName, fieldType, v);
        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(init, FlowType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(put, FlowType.FORWARD);
    }

    private void parse_NEW(final ClassDesc type, final Frame frame) {
        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = frame.copyIncomingToOutgoing();

        final New n = new New(init);

        outgoing.control = outgoing.control.controlFlowsTo(init, FlowType.FORWARD);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(n);

        outgoing.push(n);
    }

    private void parse_RETURN(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertEmptyStack(outgoing);

        final Return next = new Return();
        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_ARETURN(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        if (outgoing.stack.size() != 1) {
            illegalState("Expecting only one value on the stack");
        }

        final Value v = outgoing.pop();

        final ReturnValue next = new ReturnValue(methodTypeDesc.returnType(), v);
        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_RETURN_X(final Frame frame, final ClassDesc type) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        if (outgoing.stack.size() != 1) {
            illegalState("Expecting only one value on the stack, but got " + outgoing.stack.size());
        }

        final Value v = outgoing.pop();
        if (methodTypeDesc.returnType().isPrimitive()) {
            if (!TypeUtils.jvmInternalTypeOf(v.type).equals(TypeUtils.jvmInternalTypeOf(methodTypeDesc.returnType()))) {
                illegalState("Cannot return value of type " + TypeUtils.toString(v.type) + " as " + TypeUtils.toString(methodTypeDesc.returnType()) + " is expected!");
            }
        }

        // TODO: Does not 100% match the method signature type here
        final ReturnValue next = new ReturnValue(v.type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
    }

    private void parse_CHECKCAST(final ClassDesc typeToCheck, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value objectToCheck = outgoing.peek();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(typeToCheck);

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.control = outgoing.control.controlFlowsTo(new CheckCast(objectToCheck, classInit), FlowType.FORWARD);
    }

    private void parse_INSTANCEOF(final ClassDesc typeToCheck, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value objectToCheck = outgoing.pop();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(typeToCheck);

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, FlowType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.push(new InstanceOf(objectToCheck, classInit));
    }

    private void parse_DUP(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.peek();
        outgoing.push(v);
    }

    private void parse_DUP_X1(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value v1 = outgoing.pop();
        final Value v2 = outgoing.pop();
        outgoing.push(v1);
        outgoing.push(v2);
        outgoing.push(v1);
    }

    private void parse_DUP_X2(final Frame frame) {
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

    private void parse_DUP2(final Frame frame) {
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

    private void parse_DUP2_X1(final Frame frame) {
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

    private void parse_DUP2_X2(final Frame frame) {
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
            if (!TypeUtils.isCategory2(v1.type) && !TypeUtils.isCategory2(v2.type) && TypeUtils.isCategory2(v3.type)) {
                // Form 3
                outgoing.push(v2);
                outgoing.push(v1);
                outgoing.push(v3);
                outgoing.push(v2);
                outgoing.push(v1);
            } else if (TypeUtils.isCategory2(v1.type) && !TypeUtils.isCategory2(v2.type) && (!TypeUtils.isCategory2(v3.type))) {
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

    private void parse_POP(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        outgoing.pop();
    }

    private void parse_POP2(final Frame frame) {
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

    private void parse_SWAP(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value a = outgoing.pop();
        final Value b = outgoing.pop();
        outgoing.push(a);
        outgoing.push(b);
    }

    private void parse_ADD_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        final Add add = new Add(desc, value1, value2);
        frame.out.push(add);
    }

    private void parse_SUB_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        final Sub sub = new Sub(desc, value1, value2);
        frame.out.push(sub);
    }

    private void parse_MUL_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        final Mul mul = new Mul(desc, value1, value2);
        frame.out.push(mul);
    }

    private void parse_ARRAYLENGTH(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value array = outgoing.pop();
        outgoing.push(new ArrayLength(array));
    }

    private void parse_NEG_X(final Frame frame, final ClassDesc desc) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        final Value a = outgoing.pop();
        outgoing.push(new Negate(desc, a));
    }

    private void parse_DIV_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        final Div div = new Div(desc, value1, value2);

        final Status outgoing = frame.out;
        outgoing.control = outgoing.control.controlFlowsTo(div, FlowType.FORWARD);
        outgoing.push(div);
        frame.entryPoint = div;
    }

    private void parse_REM_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc) {
        final Rem rem = new Rem(desc, value1, value2);
        final Status outgoing = frame.out;
        outgoing.control = outgoing.control.controlFlowsTo(rem, FlowType.FORWARD);
        outgoing.push(rem);
    }

    private void parse_BITOPERATION_X(final Frame frame, final Value value1, final Value value2, final ClassDesc desc, final BitOperation.Operation operation) {
        final BitOperation rem = new BitOperation(desc, operation, value1, value2);
        frame.out.push(rem);
    }

    protected void parse_NUMERICCOMPARE_X(final Frame frame, final Value value1, final Value value2, final ClassDesc compareType, final NumericCompare.Mode mode) {
        final NumericCompare compare = new NumericCompare(mode, compareType, value1, value2);
        frame.out.push(compare);
    }

    private void parse_MONITORENTER(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorEnter(v), FlowType.FORWARD);
    }

    private void parse_MONITOREXIT(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 1);

        final Value v = outgoing.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorExit(v), FlowType.FORWARD);
    }

    private void parse_ARRAYSTORE_X_TRUNCATED(final Opcode opcode, final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 3);

        final Value value = outgoing.pop();
        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        if (!value.type.equals(ConstantDescs.CD_int)) {
            illegalState("Expected value of type int for " + opcode);
        }

        final ClassDesc arrayType = (ClassDesc) array.type;

        final ArrayStore store = new ArrayStore(array, index, new Truncate(arrayType.componentType(), value));

        outgoing.memory = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, FlowType.FORWARD);
    }

    private void parse_ASTORE_X(final Frame frame, final ClassDesc arrayType) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 3);

        final Value value = outgoing.pop();
        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ArrayStore store = new ArrayStore(array, index, value);

        outgoing.memory = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, FlowType.FORWARD);
    }

    private void parse_AASTORE(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 3);

        final Value value = outgoing.pop();
        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ArrayStore store = new ArrayStore(array, index, value);

        outgoing.memory = outgoing.memory.memoryFlowsTo(store);
        outgoing.control = outgoing.control.controlFlowsTo(store, FlowType.FORWARD);
    }

    private void parse_ALOAD_X_INTEXTENDED(final Frame frame, final Extend.ExtendType type) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ClassDesc arrayType = (ClassDesc) array.type;

        final Value load = new ArrayLoad(arrayType, array, index);

        final Value value = new Extend(ConstantDescs.CD_int, type, load);

        outgoing.memory = outgoing.memory.memoryFlowsTo(load);
        outgoing.control = outgoing.control.controlFlowsTo(load, FlowType.FORWARD);
        outgoing.push(value);
    }

    private void parse_ALOAD_X(final Frame frame, final ClassDesc arrayType) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value index = outgoing.pop();
        final Value array = outgoing.pop();
        final Value value = new ArrayLoad(arrayType, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, FlowType.FORWARD);
        outgoing.push(value);
    }

    private void parse_AALOAD(final Frame frame) {
        final Status outgoing = frame.copyIncomingToOutgoing();
        assertMinimumStackSize(outgoing, 2);

        final Value index = outgoing.pop();
        final Value array = outgoing.pop();

        final ClassDesc arrayType = (ClassDesc) array.type;

        final Value value = new ArrayLoad(arrayType, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.control = outgoing.control.controlFlowsTo(value, FlowType.FORWARD);
        outgoing.push(value);
    }

    private void parse_CONVERT_X(final Frame frame, final ClassDesc from, final ClassDesc to) {
        final Status outgoing = frame.copyIncomingToOutgoing();

        outgoing.push(new Convert(to, outgoing.pop(), from));
    }

    public Method ir() {
        return ir;
    }

    private void step4PeepholeOptimizations() {
    }

    public record NamedProjection(String name) {
        public static final NamedProjection DEFAULT = new NamedProjection("default");
    }

    public record CFGEdge(int fromIndex, NamedProjection projection, FlowType flowType) {
    }

    private record CFGAnalysisJob(int startIndex, List<Integer> path) {
    }

    public static class Frame {

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

    public static class Status {

        protected final static int UNDEFINED_LINE_NUMBER = -1;

        protected int lineNumber;
        private final Value[] locals;
        protected final Stack<Value> stack;
        protected Node control;
        protected Node memory;
        protected final Stack<ExceptionGuard> activeExceptionGuards;

        protected Status(final int maxLocals) {
            this.locals = new Value[maxLocals];
            this.stack = new Stack<>();
            this.lineNumber = UNDEFINED_LINE_NUMBER;
            this.activeExceptionGuards = new Stack<>();
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
            result.activeExceptionGuards.addAll(activeExceptionGuards);
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

        protected void registerExceptionGuard(final ExceptionGuard guard) {
            activeExceptionGuards.add(guard);
        }

        protected ExceptionGuard popExceptionGuard() {
            if (activeExceptionGuards.isEmpty()) {
                throw new IllegalStateException("No exception guard to pop!");
            }
            return activeExceptionGuards.peek();
        }
    }
}
