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

    record CFGEdge(int fromIndex, ControlType controlType) {
    }

    private record CFGAnalysisJob(int startIndex, List<Integer> path) {
    }

    public static class Frame {

        final List<CFGEdge> predecessors;
        final int elementIndex;

        public Frame(final int elementIndex) {
            this.predecessors = new ArrayList<>();
            this.elementIndex = elementIndex;
        }
    }

    private final ClassDesc owner;
    private final MethodModel method;
    private Frame[] frames;
    private final Map<Label, Status> incomingStatus;
    private final Map<Label, Integer> labelToIndex;
    private final Method ir;
    private List<Frame> codeModelTopologicalOrder;

    public MethodAnalyzer(final ClassDesc owner, final MethodModel method) {

        this.owner = owner;
        this.method = method;
        this.ir = new Method();
        this.incomingStatus = new HashMap<>();
        this.labelToIndex = new HashMap<>();

        final Optional<CodeModel> optCode = method.code();
        if (optCode.isPresent()) {
            final CodeModel code = optCode.get();
            step1AnalyzeCFG(code);
            step2ComputeTopologicalOrder();
            step3FollowCFGAndInterpret(code);
            step4MarkBackEdges();
            //step4PeepholeOptimizations();
        }
    }

    private void illegalState(final String message) {
        throw new IllegalParsingStateException(this, message);
    }

    MethodModel getMethod() {
        return method;
    }

    public Frame[] getFrames() {
        return frames;
    }

    public List<Frame> getCodeModelTopologicalOrder() {
        return codeModelTopologicalOrder;
    }

    private void step1AnalyzeCFG(final CodeModel code) {

        frames = new Frame[code.elementList().size()];
        frames[0] = new Frame(0);

        // We first need to map the labels to the instruction index
        final List<CodeElement> codeElements = code.elementList();
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
                        if (instruction.opcode() == Opcode.GOTO) {
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
                                    frame = new Frame(newIndex);
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, controlType));
                                // Analysis of the current task ends here
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
                                    frame = new Frame(newIndex);
                                    frames[newIndex] = frame;
                                }
                                frame.predecessors.add(new CFGEdge(i, controlType));
                            } else {
                                illegalState("Unconditional branch to " + branch.target() + " which is not mapped to an index");
                            }
                        }
                    } else {
                        // The following opcode ends the analysis of the current job, as
                        // controlflow terminates
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

                Frame frame = frames[i + 1];
                if (frame == null) {
                    frame = new Frame(i + 1);
                    frames[i + 1] = frame;
                }
                // This is a regular forward flow
                frame.predecessors.add(new CFGEdge(i, ControlType.FORWARD));

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

    }

    private void step3FollowCFGAndInterpret(final CodeModel code) {

        final Deque<InterpretTask> tasks = new ArrayDeque<>();
        final CodeElement start = code.elementList().getFirst();

        final CodeAttribute cm = (CodeAttribute) code;

        final Status initStatus = new Status();
        initStatus.locals = new Value[cm.maxLocals()];
        initStatus.stack = new Stack<>();
        initStatus.control = ir;
        initStatus.memory = ir;

        // Locals anhand der Methodensignatur vorinitialisieren
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

        final InterpretTask initialTask = new InterpretTask();
        initialTask.eleementIndex = 0;
        initialTask.status = initStatus;
        tasks.push(initialTask);

        final Set<Integer> visited = new HashSet<>();
        visited.add(0);

        final List<CodeElement> allElements = code.elementList();

        final Map<Label, CodeElement> labelToCodeElements = new HashMap<>();
        for (final CodeElement elem : allElements) {
            if (elem instanceof final LabelTarget labelTarget) {
                labelToCodeElements.put(labelTarget.label(), elem);
            }
        }

        while (!tasks.isEmpty()) {
            final InterpretTask task = tasks.pop();
            Status status = task.status;

            System.out.print("Analyzing basic block at #");
            System.out.print(task.eleementIndex);
            System.out.println();

            for (int elementIndex = task.eleementIndex; elementIndex < allElements.size(); elementIndex++) {
                final CodeElement current = allElements.get(elementIndex);

                if (current instanceof final LabelTarget labelnode && !visited.contains(elementIndex)) {
                    visited.add(elementIndex);

                    final InterpretTask newTask = new InterpretTask();
                    newTask.eleementIndex = allElements.indexOf(labelnode);
                    newTask.status = incomingStatusFor(status, labelnode.label());

                    // Create PHI assignments / initializations here...
                    final Frame frame = frames[newTask.eleementIndex];
                    if (frame != null && frame.predecessors.size() > 1) {
                        // TODO: What about the stack here?
                        for (int i = 0; i < newTask.status.locals.length; i++) {
                            final Value v = newTask.status.locals[i];
                            if (v != null && v != status.locals[i]) {
                                //final Copy next = new Copy(status.locals[i], v);
                                v.use(status.locals[i], new PHIUse(status.control));
                            }
                        }
                    }

                    // Keep control flow
                    final LabelNode next = ir.createLabel(labelnode.label());
                    newTask.status.control = status.control.controlFlowsTo(next, ControlType.FORWARD, task.condition);
                    task.condition = ControlFlowConditionDefault.INSTANCE;

                    tasks.push(newTask);

                    break;

                } else if (current instanceof final LabelTarget labelTarget && current == start) {

                    final LabelNode next = ir.createLabel(labelTarget.label());
                    status.control = status.control.controlFlowsTo(next, ControlType.FORWARD);

                }

                System.out.print("#");
                System.out.print(allElements.indexOf(current));
                System.out.print(" : ");

                System.out.println(current);

                System.out.print("[Line ");
                System.out.print(status.lineNumber);

                for (int i = 0; i < status.locals.length; i++) {
                    if (status.locals[i] != null) {
                        System.out.print(" Local ");
                        System.out.print(i);
                        System.out.print("=");
                        System.out.print(status.locals[i]);
                    }
                }

                for (int i = 0; i < status.stack.size(); i++) {
                    System.out.print(" Stack ");
                    System.out.print(i);
                    System.out.print("=");
                    System.out.print(status.stack.get(i));
                }

                System.out.print("]");

                status = visitNode(current, status);

                visited.add(elementIndex);

                if (current instanceof final BranchInstruction jumpInsnNode) {
                    final int jumpIndex = allElements.indexOf(labelToCodeElements.get(jumpInsnNode.target()));
                    if (!visited.contains(jumpIndex)) {
                        final InterpretTask newTask = new InterpretTask();
                        // TODO: Check if this holds true!!
                        newTask.eleementIndex = jumpIndex;
                        newTask.status = incomingStatusFor(status, jumpInsnNode.target());
                        switch (jumpInsnNode.opcode()) {
                            case Opcode.IFEQ:
                            case Opcode.IFNE:
                            case Opcode.IFLT:
                            case Opcode.IFGE:
                            case Opcode.IFGT:
                            case Opcode.IFLE:
                            case Opcode.IFNULL:
                            case Opcode.IFNONNULL:
                            case Opcode.IF_ACMPEQ:
                            case Opcode.IF_ACMPNE:
                            case Opcode.IF_ICMPEQ:
                            case Opcode.IF_ICMPNE:
                            case Opcode.IF_ICMPLT:
                            case Opcode.IF_ICMPGE:
                            case Opcode.IF_ICMPGT:
                            case Opcode.IF_ICMPLE: {
                                newTask.condition = ControlFlowConditionOnTrue.INSTANCE;
                                break;
                            }
                        }
                        // TODO: Check for PHI propagation
                        tasks.push(newTask);
                    }
                    if (jumpInsnNode.opcode() == Opcode.GOTO || jumpInsnNode.opcode() == Opcode.GOTO_W) {
                        // Unconditional jump and a back edge, we stop analysis here
                        final Status target = incomingStatus.get(jumpInsnNode.target());
                        if (target == null) {
                            illegalState("Unconditional jump to " + jumpInsnNode.target() + " without a target status");
                        }
                        // Do some sanity checking here
                        for (int i = 0; i < status.locals.length; i++) {
                            final Value source = status.locals[i];
                            final Value dest = target.locals[i];
                            if (dest != null && dest != source) {
                                illegalState("Missing copy for local index " + i + ", got " + source + " and expected " + dest);
                            }
                        }
                        break;
                    }
                }
                if (current instanceof ThrowInstruction) {
                    break;
                } else if (current instanceof final ReturnInstruction returnInstruction) {
                    if (returnInstruction.opcode() == Opcode.RETURN) {
                        break;
                    }
                    if (returnInstruction.opcode() == Opcode.IRETURN) {
                        break;
                    }
                    if (returnInstruction.opcode() == Opcode.LRETURN) {
                        break;
                    }
                    if (returnInstruction.opcode() == Opcode.FRETURN) {
                        break;
                    }
                    if (returnInstruction.opcode() == Opcode.DRETURN) {
                        break;
                    }
                    if (returnInstruction.opcode() == Opcode.ARETURN) {
                        break;
                    }
                }

                if (visited.contains(elementIndex + 1)) {
                    break;
                }
            }
            System.out.println("Basic block finished");
        }
    }

    private void step4MarkBackEdges() {
        ir.markBackEdges();
    }

    boolean needsTwoSlots(final ClassDesc type) {
        return type.equals(ConstantDescs.CD_long) || type.equals(ConstantDescs.CD_double);
    }

    private Status incomingStatusFor(final Status status, final Label label) {
        return incomingStatus.computeIfAbsent(label, key -> {
            if (!labelToIndex.containsKey(label)) {
                throw new IllegalArgumentException("No label index for " + label);
            }
            final Frame frame = frames[labelToIndex.get(label)];
            if (frame == null || frame.predecessors.isEmpty()) {
                illegalState("No predecessor for " + key);
            }
            if (frame.predecessors.size() > 1) {
                // PHI Values
                return status.copyWithPHI(ir.createLabel(key));
            }
            return status.copy();
        });
    }

    private Status visitNode(final CodeElement node, final Status incoming) {

        if (node instanceof final PseudoInstruction psi) {
            // Pseudo Instructions
            return switch (psi) {
                case final LabelTarget labelTarget -> visitLabelTarget(labelTarget, incoming);
                case final LineNumber lineNumber -> visitLineNumberNode(lineNumber, incoming);
                case final LocalVariable localVariable ->
                    // Maybe we can use this for debugging?
                        incoming;
                case final LocalVariableType localVariableType -> incoming;
                case final ExceptionCatch exceptionCatch -> visitExceptionCatch(exceptionCatch, incoming);
                default -> throw new IllegalArgumentException("Not implemented yet : " + psi);
            };
        } else if (node instanceof final Instruction ins) {
            // Real bytecode instructions
            return switch (ins) {
                case final IncrementInstruction incrementInstruction -> parse_IINC(incrementInstruction, incoming);
                case final InvokeInstruction invokeInstruction -> visitInvokeInstruction(invokeInstruction, incoming);
                case final LoadInstruction load -> visitLoadInstruction(load, incoming);
                case final StoreInstruction store -> visitStoreInstruction(store, incoming);
                case final BranchInstruction branchInstruction -> visitBranchInstruction(branchInstruction, incoming);
                case final ConstantInstruction constantInstruction ->
                        visitConstantInstruction(constantInstruction, incoming);
                case final FieldInstruction fieldInstruction -> visitFieldInstruction(fieldInstruction, incoming);
                case final NewObjectInstruction newObjectInstruction ->
                        visitNewObjectInstruction(newObjectInstruction, incoming);
                case final ReturnInstruction returnInstruction -> visitReturnInstruction(returnInstruction, incoming);
                case final InvokeDynamicInstruction invokeDynamicInstruction ->
                        parse_INVOKEDYNAMIC(invokeDynamicInstruction, incoming);
                case final TypeCheckInstruction typeCheckInstruction ->
                        visitTypeCheckInstruction(typeCheckInstruction, incoming);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction, incoming);
                case final OperatorInstruction operatorInstruction ->
                        visitOperatorInstruction(operatorInstruction, incoming);
                case final MonitorInstruction monitorInstruction ->
                        visitMonitorInstruction(monitorInstruction, incoming);
                case final ThrowInstruction thr -> visitThrowInstruction(thr, incoming);
                case final NewPrimitiveArrayInstruction na -> visitNewPrimitiveArray(na, incoming);
                case final ArrayStoreInstruction as -> visitArrayStoreInstruction(as, incoming);
                case final ArrayLoadInstruction al -> visitArrayLoadInstruction(al, incoming);
                case final NopInstruction nop -> visitNopInstruction(nop, incoming);
                case final NewReferenceArrayInstruction rei -> visitNewObjectArray(rei, incoming);
                case final ConvertInstruction ci -> visitConvertInstruction(ci, incoming);
                case final NewMultiArrayInstruction nm -> visitNewMultiArray(nm, incoming);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            };
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private Status visitLabelTarget(final LabelTarget node, final Status incoming) {
        // An AbstractInsnNode that encapsulates a Label.
        System.out.print("  Label: " + node.label());
        final Frame frame = frames[labelToIndex.get(node.label())];
        if (frame != null && !frame.predecessors.isEmpty()) {
            System.out.print(" Jumped from [");
            for (int i = 0; i < frame.predecessors.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(frame.predecessors.get(i).fromIndex);
            }
            System.out.print("]");
        }
        System.out.println();

        return incoming;
    }

    private Status visitLineNumberNode(final LineNumber node, final Status incoming) {
        // A node that represents a line number declaration. These nodes are pseudo-instruction nodes inorder to be inserted in an instruction list.
        final Status n = incoming.copy();
        n.lineNumber = node.line();
        System.out.println("  Line " + node.line());
        return n;
    }

    private Status visitExceptionCatch(final ExceptionCatch node, final Status incoming) {
        return incoming;
    }

    private Status parse_IINC(final IncrementInstruction node, final Status incoming) {
        // A node that represents an IINC instruction.
        System.out.println("  opcode IINC Local " + node.slot() + " Increment " + node.constant());

        final Status outgoing = incoming.copy();
        outgoing.locals[node.slot()] = new Add(ConstantDescs.CD_int, incoming.locals[node.slot()], ir.definePrimitiveInt(node.constant()));
        return outgoing;
    }

    private Status visitInvokeInstruction(final InvokeInstruction node, final Status incoming) {
        // A node that represents a method instruction. A method instruction is an instruction that invokes a method.
        return switch (node.opcode()) {
            case Opcode.INVOKESPECIAL -> parse_INVOKESPECIAL(node, incoming);
            case Opcode.INVOKEVIRTUAL -> parse_INVOKEVIRTUAL(node, incoming);
            case Opcode.INVOKEINTERFACE -> parse_INVOKEINTERFACE(node, incoming);
            case Opcode.INVOKESTATIC -> parse_INVOKESTATIC(node, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status visitLoadInstruction(final LoadInstruction node, final Status incoming) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        return switch (node.opcode()) {
            case Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_2, Opcode.ALOAD_1, Opcode.ALOAD_0 ->
                    parse_ALOAD(node, incoming);
            case Opcode.ILOAD, Opcode.ILOAD_3, Opcode.ILOAD_2, Opcode.ILOAD_1, Opcode.ILOAD_0 ->
                    parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_int);
            case Opcode.DLOAD, Opcode.DLOAD_3, Opcode.DLOAD_2, Opcode.DLOAD_1, Opcode.DLOAD_0 ->
                    parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_double);
            case Opcode.FLOAD, Opcode.FLOAD_3, Opcode.FLOAD_2, Opcode.FLOAD_1, Opcode.FLOAD_0 ->
                    parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_float);
            case Opcode.LLOAD, Opcode.LLOAD_3, Opcode.LLOAD_2, Opcode.LLOAD_1, Opcode.LLOAD_0 ->
                    parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status visitStoreInstruction(final StoreInstruction node, final Status incoming) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        return switch (node.opcode()) {
            case Opcode.ASTORE, Opcode.ASTORE_3, Opcode.ASTORE_2, Opcode.ASTORE_1, Opcode.ASTORE_0, Opcode.ASTORE_W ->
                    parse_ASTORE(node, incoming);
            case Opcode.ISTORE, Opcode.ISTORE_3, Opcode.ISTORE_2, Opcode.ISTORE_1, Opcode.ISTORE_0, Opcode.ISTORE_W ->
                    parse_STORE_TYPE(node, incoming, ConstantDescs.CD_int);
            case Opcode.LSTORE, Opcode.LSTORE_3, Opcode.LSTORE_2, Opcode.LSTORE_1, Opcode.LSTORE_0, Opcode.LSTORE_W ->
                    parse_STORE_TYPE(node, incoming, ConstantDescs.CD_long);
            case Opcode.FSTORE, Opcode.FSTORE_0, Opcode.FSTORE_1, Opcode.FSTORE_2, Opcode.FSTORE_3, Opcode.FSTORE_W ->
                    parse_STORE_TYPE(node, incoming, ConstantDescs.CD_float);
            case Opcode.DSTORE, Opcode.DSTORE_0, Opcode.DSTORE_1, Opcode.DSTORE_2, Opcode.DSTORE_3, Opcode.DSTORE_W ->
                    parse_STORE_TYPE(node, incoming, ConstantDescs.CD_double);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status visitBranchInstruction(final BranchInstruction node, final Status incoming) {
        // A node that represents a jump instruction. A jump instruction is an instruction that may jump to another instruction.
        return switch (node.opcode()) {
            case Opcode.IF_ICMPEQ -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.EQ);
            case Opcode.IF_ICMPNE -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.NE);
            case Opcode.IF_ICMPGE -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.GE);
            case Opcode.IF_ICMPLE -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.LE);
            case Opcode.IF_ICMPLT -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.LT);
            case Opcode.IF_ICMPGT -> parse_IF_ICMP_OP(node, incoming, NumericCondition.Operation.GT);
            case Opcode.IFEQ -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.EQ);
            case Opcode.IFNE -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.NE);
            case Opcode.IFLT -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.LT);
            case Opcode.IFGE -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.GE);
            case Opcode.IFGT -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.GT);
            case Opcode.IFLE -> parse_IF_NUMERIC_X(node, incoming, NumericCondition.Operation.LE);
            case Opcode.IFNULL -> parse_IF_REFERENCETEST_X(node, incoming, ReferenceTest.Operation.NULL);
            case Opcode.IFNONNULL -> parse_IF_REFERENCETEST_X(node, incoming, ReferenceTest.Operation.NONNULL);
            case Opcode.IF_ACMPEQ -> parse_IF_ACMP_OP(node, incoming, ReferenceCondition.Operation.EQ);
            case Opcode.IF_ACMPNE -> parse_IF_ACMP_OP(node, incoming, ReferenceCondition.Operation.NE);
            case Opcode.GOTO -> parse_GOTO(node, incoming);
            case Opcode.GOTO_W -> parse_GOTO(node, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status visitConstantInstruction(final ConstantInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.LDC -> parse_LDC(ins, incoming);
            case Opcode.LDC_W -> parse_LDC(ins, incoming);
            case Opcode.LDC2_W -> parse_LDC(ins, incoming);
            case Opcode.ICONST_M1, Opcode.ICONST_0, Opcode.ICONST_5, Opcode.ICONST_4, Opcode.ICONST_3, Opcode.ICONST_2,
                 Opcode.ICONST_1 -> parse_ICONST(ins, incoming);
            case Opcode.LCONST_0, Opcode.LCONST_1 -> parse_LCONST(ins, incoming);
            case Opcode.FCONST_0, Opcode.FCONST_1, Opcode.FCONST_2 -> parse_FCONST(ins, incoming);
            case Opcode.DCONST_0, Opcode.DCONST_1 -> parse_DCONST(ins, incoming);
            case Opcode.BIPUSH -> parse_BIPUSH(ins, incoming);
            case Opcode.SIPUSH -> parse_SIPUSH(ins, incoming);
            case Opcode.ACONST_NULL -> parse_ACONST_NULL(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitFieldInstruction(final FieldInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case GETFIELD -> parse_GETFIELD(ins, incoming);
            case PUTFIELD -> parse_PUTFIELD(ins, incoming);
            case GETSTATIC -> parse_GETSTATIC(ins, incoming);
            case PUTSTATIC -> parse_PUTSTATIC(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitNewObjectInstruction(final NewObjectInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.NEW -> parse_NEW(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitReturnInstruction(final ReturnInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.RETURN -> parse_RETURN(ins, incoming);
            case Opcode.ARETURN -> parse_ARETURN(ins, incoming);
            case Opcode.IRETURN -> parse_RETURN_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.DRETURN -> parse_RETURN_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.FRETURN -> parse_RETURN_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.LRETURN -> parse_RETURN_X(ins, incoming, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status parse_INVOKEDYNAMIC(final InvokeDynamicInstruction node, final Status incoming) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();
        System.out.println("  opcode INVOKEDYNAMIC Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }
        final Status outgoing = incoming.copy();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
        }
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            // TODO: Create invocation here
            outgoing.stack.push(new Result(returnType));
        }
        return outgoing;
    }

    private Status visitTypeCheckInstruction(final TypeCheckInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case CHECKCAST -> parse_CHECKCAST(ins, incoming);
            case INSTANCEOF -> parse_INSTANCEOF(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitStackInstruction(final StackInstruction ins, final Status incominmg) {
        return switch (ins.opcode()) {
            case Opcode.DUP -> parse_DUP(ins, incominmg);
            case Opcode.DUP_X1 -> parse_DUP_X1(ins, incominmg);
            case Opcode.DUP_X2 -> parse_DUP_X2(ins, incominmg);
            case Opcode.DUP2 -> parse_DUP2(ins, incominmg);
            case Opcode.DUP2_X1 -> parse_DUP2_X1(ins, incominmg);
            case Opcode.DUP2_X2 -> parse_DUP2_X2(ins, incominmg);
            case Opcode.POP -> parse_POP(ins, incominmg);
            case Opcode.POP2 -> parse_POP2(ins, incominmg);
            case Opcode.SWAP -> parse_SWAP(ins, incominmg);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitOperatorInstruction(final OperatorInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.IADD -> parse_ADD_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.DADD -> parse_ADD_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.FADD -> parse_ADD_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.LADD -> parse_ADD_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.DSUB -> parse_SUB_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.FSUB -> parse_SUB_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.ISUB -> parse_SUB_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.LSUB -> parse_SUB_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.DMUL -> parse_MUL_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.FMUL -> parse_MUL_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.IMUL -> parse_MUL_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.LMUL -> parse_MUL_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.ARRAYLENGTH -> parse_ARRAYLENGTH(ins, incoming);
            case Opcode.INEG -> parse_NEG_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.LNEG -> parse_NEG_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.FNEG -> parse_NEG_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.DNEG -> parse_NEG_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.IDIV -> parse_DIV_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.LDIV -> parse_DIV_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.FDIV -> parse_DIV_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.DDIV -> parse_DIV_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.IREM -> parse_REM_X(ins, incoming, ConstantDescs.CD_int);
            case Opcode.LREM -> parse_REM_X(ins, incoming, ConstantDescs.CD_long);
            case Opcode.FREM -> parse_REM_X(ins, incoming, ConstantDescs.CD_float);
            case Opcode.DREM -> parse_REM_X(ins, incoming, ConstantDescs.CD_double);
            case Opcode.IAND -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.AND);
            case Opcode.IOR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.OR);
            case Opcode.IXOR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.XOR);
            case Opcode.ISHL -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.SHL);
            case Opcode.ISHR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.SHR);
            case Opcode.IUSHR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_int, BitOperation.Operation.USHR);
            case Opcode.LAND -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.AND);
            case Opcode.LOR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.OR);
            case Opcode.LXOR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.XOR);
            case Opcode.LSHL -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.SHL);
            case Opcode.LSHR -> parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.SHR);
            case Opcode.LUSHR ->
                    parse_BITOPERATION_X(ins, incoming, ConstantDescs.CD_long, BitOperation.Operation.USHR);
            case Opcode.FCMPG -> parse_NUMERICCOMPARE_X(ins, incoming, NumericCompare.Mode.NAN_IS_1);
            case Opcode.FCMPL -> parse_NUMERICCOMPARE_X(ins, incoming, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.DCMPG -> parse_NUMERICCOMPARE_X(ins, incoming, NumericCompare.Mode.NAN_IS_1);
            case Opcode.DCMPL -> parse_NUMERICCOMPARE_X(ins, incoming, NumericCompare.Mode.NAN_IS_MINUS_1);
            case Opcode.LCMP -> parse_NUMERICCOMPARE_X(ins, incoming, NumericCompare.Mode.NONFLOATINGPOINT);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitMonitorInstruction(final MonitorInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.MONITORENTER -> parse_MONITORENTER(ins, incoming);
            case Opcode.MONITOREXIT -> parse_MONITOREXIT(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitThrowInstruction(final ThrowInstruction ins, final Status incoming) {
        System.out.println("  " + ins + " opcode " + ins.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        final Throw t = new Throw(v);
        outgoing.control = outgoing.control.controlFlowsTo(t, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(t);
        return outgoing;
    }

    private Status visitNewPrimitiveArray(final NewPrimitiveArrayInstruction ins, final Status incoming) {
        System.out.println("  " + ins + " opcode " + ins.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value length = outgoing.stack.pop();
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
        outgoing.stack.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        return outgoing;
    }

    private Status visitArrayStoreInstruction(final ArrayStoreInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.BASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_byte.arrayType());
            case Opcode.CASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_char.arrayType());
            case Opcode.SASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_short.arrayType());
            case Opcode.IASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_int.arrayType());
            case Opcode.LASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_long.arrayType());
            case Opcode.FASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_float.arrayType());
            case Opcode.DASTORE -> parse_ASTORE_X(ins, incoming, ConstantDescs.CD_double.arrayType());
            case Opcode.AASTORE -> parse_AASTORE(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitArrayLoadInstruction(final ArrayLoadInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.BALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_byte.arrayType(), ConstantDescs.CD_int); // Sign extend!
            case Opcode.CALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_char.arrayType(), ConstantDescs.CD_int); // Zero extend!
            case Opcode.SALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_short.arrayType(), ConstantDescs.CD_int); // Sign extend!
            case Opcode.IALOAD -> parse_ALOAD_X(ins, incoming, ConstantDescs.CD_int.arrayType(), ConstantDescs.CD_int);
            case Opcode.LALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_long.arrayType(), ConstantDescs.CD_long);
            case Opcode.FALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_float.arrayType(), ConstantDescs.CD_float);
            case Opcode.DALOAD ->
                    parse_ALOAD_X(ins, incoming, ConstantDescs.CD_double.arrayType(), ConstantDescs.CD_double);
            case Opcode.AALOAD -> parse_AALOAD(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitNopInstruction(final NopInstruction ins, final Status incoming) {
        return incoming.copy();
    }

    private Status visitNewObjectArray(final NewReferenceArrayInstruction ins, final Status incoming) {
        System.out.println("  " + ins + " opcode " + ins.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value length = outgoing.stack.pop();
        final ClassDesc type = ins.componentType().asSymbol().arrayType();
        final NewArray newArray = new NewArray(type, length);
        outgoing.stack.push(newArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newArray);
        return outgoing;
    }

    private Status visitConvertInstruction(final ConvertInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.I2B -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_byte);
            case Opcode.I2C -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_char);
            case Opcode.I2S -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_short);
            case Opcode.I2L -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_long);
            case Opcode.I2F -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_float);
            case Opcode.I2D -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_int, ConstantDescs.CD_double);
            case Opcode.L2I -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_long, ConstantDescs.CD_int);
            case Opcode.L2F -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_long, ConstantDescs.CD_float);
            case Opcode.L2D -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_long, ConstantDescs.CD_double);
            case Opcode.F2I -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_float, ConstantDescs.CD_int);
            case Opcode.F2L -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_float, ConstantDescs.CD_long);
            case Opcode.F2D -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_float, ConstantDescs.CD_double);
            case Opcode.D2I -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_double, ConstantDescs.CD_int);
            case Opcode.D2L -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_double, ConstantDescs.CD_long);
            case Opcode.D2F -> parse_CONVERT_X(ins, incoming, ConstantDescs.CD_double, ConstantDescs.CD_float);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitNewMultiArray(final NewMultiArrayInstruction ins, final Status incoming) {
        System.out.println("  " + ins + " opcode " + ins.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot throw with empty stack");
        }
        final Status outgoing = incoming.copy();
        ClassDesc type = ins.arrayType().asSymbol();
        final List<Value> dimensions = new ArrayList<>();
        for (int i = 0; i < ins.dimensions(); i++) {
            dimensions.add(outgoing.stack.pop());
            type = type.arrayType();
        }
        final NewMultiArray newMultiArray = new NewMultiArray(type, dimensions);
        outgoing.stack.push(newMultiArray);
        outgoing.memory = outgoing.memory.memoryFlowsTo(newMultiArray);
        return outgoing;
    }

    private Status parse_INVOKESPECIAL(final InvokeInstruction node, final Status incoming) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final Status outgoing = incoming.copy();

        System.out.println("  opcode INVOKESPECIAL Method " + node.name() + " desc " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (outgoing.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(outgoing.stack.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(next);
        }

        return outgoing;
    }

    private Status parse_INVOKEVIRTUAL(final InvokeInstruction node, final Status incoming) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final Status outgoing = incoming.copy();

        System.out.println("  opcode INVOKEVIRTUAL Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (outgoing.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
        }
        return outgoing;
    }

    private Status parse_INVOKEINTERFACE(final InvokeInstruction node, final Status incoming) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKEINTERFACE Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }

        final Status outgoing = incoming.copy();

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
        }

        return outgoing;
    }

    private Status parse_INVOKESTATIC(final InvokeInstruction node, final Status incoming) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKESTATIC Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (incoming.stack.size() < expectedarguments) {
            illegalState("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }

        final Status outgoing = incoming.copy();

        final RuntimeclassReference runtimeClass = ir.defineRuntimeclassReference(node.method().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(runtimeClass);

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        arguments.add(init);

        final Invocation invocation = new Invocation(node, arguments.reversed());

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(invocation);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(invocation, ControlType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
        }
        return outgoing;
    }

    private Status parse_ALOAD(final LoadInstruction node, final Status incoming) {
        System.out.println("  opcode ALOAD Local " + node.slot());
        final Value v = incoming.locals[node.slot()];
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }

        final Status outgoing = incoming.copy();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_LOAD_TYPE(final LoadInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode LOAD Local " + node.slot() + " opcpde " + node.opcode());
        final Value v = incoming.locals[node.slot()];
        if (v == null) {
            illegalState("Cannot local is null for index " + node.slot());
        }
        final Status outgoing = incoming.copy();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_ASTORE(final StoreInstruction node, final Status incoming) {
        System.out.println("  opcode ASTORE Local " + node.slot());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot store empty stack");
        }
        final Status outgoing = incoming.copy();
        outgoing.locals[node.slot()] = outgoing.stack.pop();
        if (node.slot() > 0) {
            if (outgoing.locals[node.slot() - 1] != null && needsTwoSlots(outgoing.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                outgoing.locals[node.slot() - 1] = null;
            }
        }
        return outgoing;
    }

    private Status parse_STORE_TYPE(final StoreInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode STORE Local " + node.slot() + " opcode " + node.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot store empty stack");
        }
        final Status outgoing = incoming.copy();

        final Value v = outgoing.stack.pop();
        if (!v.type.equals(type)) {
            illegalState("Cannot store non " + type + " value " + v + " for index " + node.slot());
        }
        outgoing.locals[node.slot()] = v;
        if (node.slot() > 0) {
            if (outgoing.locals[node.slot() - 1] != null && needsTwoSlots(outgoing.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                outgoing.locals[node.slot() - 1] = null;
            }
        }
        return outgoing;
    }

    private Status parse_IF_ICMP_OP(final BranchInstruction node, final Status incoming, final NumericCondition.Operation op) {
        System.out.println("  opcode IFCMP_GE Target " + node.target());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for comparison");
        }

        final Status outgoing = incoming.copy();

        final Value v1 = outgoing.stack.pop();
        final Value v2 = outgoing.stack.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v1, v2);
        final If next = new If(numericCondition);
        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        // TODO: Handle jumps and copy of phi?
        return outgoing;
    }

    private Status parse_IF_NUMERIC_X(final BranchInstruction node, final Status incoming, final NumericCondition.Operation op) {
        System.out.println("  opcode " + node.opcode() + " Target " + node.target());
        if (incoming.stack.isEmpty()) {
            illegalState("Need a value on stack for comparison");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();

        final NumericCondition numericCondition = new NumericCondition(op, v, ir.definePrimitiveInt(0));
        final If next = new If(numericCondition);

        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_IF_REFERENCETEST_X(final BranchInstruction node, final Status incoming, final ReferenceTest.Operation op) {
        System.out.println("  opcode " + node.opcode() + " Target " + node.target());
        if (incoming.stack.isEmpty()) {
            illegalState("Need a value on stack for comparison");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();

        final ReferenceTest referenceCondition = new ReferenceTest(op, v);
        final If next = new If(referenceCondition);

        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_IF_ACMP_OP(final BranchInstruction node, final Status incoming, final ReferenceCondition.Operation op) {
        System.out.println("  opcode " + node.opcode() + " Target " + node.target());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for comparison");
        }

        final Status outgoing = incoming.copy();

        final Value v1 = outgoing.stack.pop();
        final Value v2 = outgoing.stack.pop();

        final ReferenceCondition condition = new ReferenceCondition(op, v1, v2);
        final If next = new If(condition);
        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        // TODO: Handle jumps and copy of phi?
        return outgoing;
    }

    private Status parse_GOTO(final BranchInstruction node, final Status incoming) {
        System.out.println("  opcode " + node.opcode() + " Target " + node.target());

        final LabelNode label = ir.createLabel(node.target());

        final Status targetIncoming = incomingStatusFor(incoming, node.target());

        final Status outgoing = incoming.copy();

        final Goto next = new Goto();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);

        for (int i = 0; i < incoming.locals.length; i++) {
            final Value v = incoming.locals[i];
            final Value target = targetIncoming.locals[i];
            if (v != null && v != target) {
                target.use(v, new PHIUse(next));
                outgoing.locals[i] = target;
            }
        }

        next.controlFlowsTo(label, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_LDC(final ConstantInstruction node, final Status incoming) {
        // A node that represents an LDC instruction.
        System.out.println("  opcode LDC Constant " + node.constantValue());
        final Status outgoing = incoming.copy();
        if (node.constantValue() instanceof final String str) {
            outgoing.stack.push(ir.defineStringConstant(str));
        } else if (node.constantValue() instanceof final Integer i) {
            outgoing.stack.push(ir.definePrimitiveInt(i));
        } else if (node.constantValue() instanceof final Long l) {
            outgoing.stack.push(ir.definePrimitiveLong(l));
        } else if (node.constantValue() instanceof final Float f) {
            outgoing.stack.push(ir.definePrimitiveFloat(f));
        } else if (node.constantValue() instanceof final Double d) {
            outgoing.stack.push(ir.definePrimitiveDouble(d));
        } else if (node.constantValue() instanceof final ClassDesc classDesc) {
            outgoing.stack.push(ir.defineRuntimeclassReference(classDesc));
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
        return outgoing;
    }

    private Status parse_ICONST(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode ICONST " + node.constantValue());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveInt((Integer) node.constantValue()));
        return outgoing;
    }

    private Status parse_LCONST(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode LCONST " + node.constantValue());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveLong((Long) node.constantValue()));
        return outgoing;
    }

    private Status parse_FCONST(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode FCONST " + node.constantValue());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveFloat((Float) node.constantValue()));
        return outgoing;
    }

    private Status parse_DCONST(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode DCONST " + node.constantValue());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveDouble((Double) node.constantValue()));
        return outgoing;
    }

    private Status parse_BIPUSH(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode " + node.opcode());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveInt((Integer) node.constantValue()));
        return outgoing;
    }

    private Status parse_SIPUSH(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode " + node.opcode());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveShort(((Number) node.constantValue()).shortValue()));
        return outgoing;
    }

    private Status parse_ACONST_NULL(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode " + node.opcode());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.defineNullReference());
        return outgoing;
    }

    private Status parse_GETFIELD(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode GETFIELD Field " + node.field());
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot load field from empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        if (v.type.isPrimitive() || v.type.isArray()) {
            illegalState("Cannot load field from non object value " + v);
        }
        final GetField get = new GetField(node, v);
        outgoing.stack.push(get);

        outgoing.memory = outgoing.memory.memoryFlowsTo(get);

        return outgoing;
    }

    private Status parse_PUTFIELD(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode PUTFIELD Field " + node.field());

        if (incoming.stack.isEmpty()) {
            illegalState("Cannot put field from empty stack");
        }

        final Status outgoing = incoming.copy();

        final Value v = outgoing.stack.pop();
        final Value target = outgoing.stack.pop();

        final PutField put = new PutField(target, node.name().stringValue(), node.typeSymbol(), v);

        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_GETSTATIC(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode GETSTATIC Field " + node.field());

        final Status outgoing = incoming.copy();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final GetStatic get = new GetStatic(ri, node.name().stringValue(), node.typeSymbol());
        outgoing.stack.push(get);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(get);
        return outgoing;
    }

    private Status parse_PUTSTATIC(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode PUTSTATIC Field " + node.field());

        if (incoming.stack.isEmpty()) {
            illegalState("Cannot put field from empty stack");
        }

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = incoming.copy();

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);

        final Value v = outgoing.stack.pop();

        final PutStatic put = new PutStatic(ri, node.name().stringValue(), node.typeSymbol(), v);
        outgoing.memory = outgoing.memory.memoryFlowsTo(put);
        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.control = outgoing.control.controlFlowsTo(put, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_NEW(final NewObjectInstruction node, final Status incoming) {
        System.out.println("  opcode NEW Creating " + node.className().asSymbol());
        final ClassDesc type = node.className().asSymbol();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = incoming.copy();

        final New n = new New(init);

        outgoing.control = outgoing.control.controlFlowsTo(init, ControlType.FORWARD);

        outgoing.memory = outgoing.memory.memoryFlowsTo(init);
        outgoing.memory = outgoing.memory.memoryFlowsTo(n);

        outgoing.stack.push(n);

        return outgoing;
    }

    private Status parse_RETURN(final ReturnInstruction node, final Status incoming) {
        System.out.println("  opcode RETURN");

        final Return next = new Return();

        final Status outgoing = incoming.copy();
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
        return outgoing;
    }

    private Status parse_ARETURN(final ReturnInstruction node, final Status incoming) {
        System.out.println("  opcode ARETURN");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot return empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();

        final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
        if (!v.type.equals(methodTypeDesc.returnType())) {
            illegalState("Expecting type " + methodTypeDesc.returnType() + " on stack, got " + v.type);
        }

        final ReturnValue next = new ReturnValue(v.type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
        return outgoing;
    }

    private Status parse_RETURN_X(final ReturnInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode RETURN");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot return empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        final ReturnValue next = new ReturnValue(type, v);
        outgoing.control = outgoing.control.controlFlowsTo(next, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(next);
        return outgoing;
    }

    private Status parse_CHECKCAST(final TypeCheckInstruction node, final Status incoming) {
        System.out.println("  opcode CHECKCAST Checking for " + node.type().asSymbol());
        if (incoming.stack.isEmpty()) {
            illegalState("Checkcast requires a stack entry");
        }
        final Status outgoing = incoming.copy();
        final Value objectToCheck = outgoing.stack.peek();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(node.type().asSymbol());

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.control = outgoing.control.controlFlowsTo(new CheckCast(objectToCheck, classInit), ControlType.FORWARD);

        return outgoing;
    }

    private Status parse_INSTANCEOF(final TypeCheckInstruction node, final Status incoming) {
        System.out.println("  opcode INSTANCEOF Checking for " + node.type().asSymbol());
        if (incoming.stack.isEmpty()) {
            illegalState("Instanceof requires a stack entry");
        }
        final Status outgoing = incoming.copy();
        final Value objectToCheck = outgoing.stack.peek();
        final RuntimeclassReference expectedType = ir.defineRuntimeclassReference(node.type().asSymbol());

        final ClassInitialization classInit = new ClassInitialization(expectedType);
        outgoing.control = outgoing.control.controlFlowsTo(classInit, ControlType.FORWARD);
        outgoing.memory = outgoing.memory.memoryFlowsTo(classInit);

        outgoing.stack.push(new InstanceOf(objectToCheck, classInit));

        return outgoing;
    }

    private Status parse_DUP(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.peek();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_DUP_X1(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP_X1");
        if (incoming.stack.size() < 2) {
            illegalState("Two stack values are required for DUP_X1");
        }
        final Status outgoing = incoming.copy();
        final Value v1 = outgoing.stack.peek();
        final Value v2 = outgoing.stack.peek();
        outgoing.stack.push(v1);
        outgoing.stack.push(v2);
        outgoing.stack.push(v1);
        return outgoing;
    }

    private Status parse_DUP_X2(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP_X2");
        if (incoming.stack.size() < 2) {
            illegalState("Two stack values are required for DUP_X2");
        }
        final Status outgoing = incoming.copy();
        final Value v1 = outgoing.stack.peek();
        final Value v2 = outgoing.stack.peek();
        if (isCategory2(v2.type) && !isCategory2(v1.type)) {
            // Form 2
            outgoing.stack.push(v1);
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
        } else {
            // Form 1
            if (outgoing.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP_X2");
            }
            final Value v3 = outgoing.stack.peek();
            if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP_X2");
            }
            outgoing.stack.push(v1);
            outgoing.stack.push(v3);
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
        }
        return outgoing;
    }

    private Status parse_DUP2(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP2");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v1 = outgoing.stack.pop();
        if (isCategory2(v1.type)) {
            outgoing.stack.push(v1);
            outgoing.stack.push(v1);
            return outgoing;
        }
        if (incoming.stack.isEmpty()) {
            illegalState("Another stack entry is required for DUP2 type 1 operation");
        }
        final Value v2 = outgoing.stack.pop();
        outgoing.stack.push(v2);
        outgoing.stack.push(v1);
        outgoing.stack.push(v2);
        outgoing.stack.push(v1);
        return outgoing;
    }

    private Status parse_DUP2_X1(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP2_X1");
        if (incoming.stack.size() < 2) {
            illegalState("A minium of two stack values are required for DUP2_X1");
        }
        final Status outgoing = incoming.copy();

        final Value v1 = outgoing.stack.pop();
        final Value v2 = outgoing.stack.pop();
        if (isCategory2(v1.type) && !isCategory2(v2.type)) {
            // Form 2
            outgoing.stack.push(v1);
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
        } else {
            // Form 1
            if (outgoing.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP2_X1");
            }
            final Value v3 = outgoing.stack.peek();
            if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type)) {
                illegalState("All values must be of category 1 type for DUP2_X1");
            }
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
            outgoing.stack.push(v3);
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
        }

        return outgoing;
    }

    private Status parse_DUP2_X2(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP2_X1");
        if (incoming.stack.size() < 2) {
            illegalState("A minium of two stack values are required for DUP2_X1");
        }
        final Status outgoing = incoming.copy();

        final Value v1 = outgoing.stack.pop();
        final Value v2 = outgoing.stack.pop();
        if (isCategory2(v1.type) && isCategory2(v2.type)) {
            // Form 4
            outgoing.stack.push(v1);
            outgoing.stack.push(v2);
            outgoing.stack.push(v1);
        } else {
            if (incoming.stack.isEmpty()) {
                illegalState("Another stack entry is required for DUP2_X1");
            }
            final Value v3 = outgoing.stack.pop();
            if (!isCategory2(v1.type) && !isCategory2(v2.type) || isCategory2(v3.type)) {
                // Form 3
                outgoing.stack.push(v2);
                outgoing.stack.push(v1);
                outgoing.stack.push(v3);
                outgoing.stack.push(v2);
                outgoing.stack.push(v1);
            } else if (isCategory2(v1.type) || !isCategory2(v2.type) && (!isCategory2(v3.type))) {
                // Form 2
                outgoing.stack.push(v1);
                outgoing.stack.push(v3);
                outgoing.stack.push(v2);
                outgoing.stack.push(v1);
            } else {
                // Form 1
                if (outgoing.stack.isEmpty()) {
                    illegalState("Another stack entry is required for DUP2_X1");
                }
                final Value v4 = outgoing.stack.peek();
                if (isCategory2(v1.type) || isCategory2(v2.type) || isCategory2(v3.type) || isCategory2(v4.type)) {
                    illegalState("All values must be of category 1 type for DUP2_X1");
                }
                outgoing.stack.push(v2);
                outgoing.stack.push(v1);
                outgoing.stack.push(v4);
                outgoing.stack.push(v3);
                outgoing.stack.push(v2);
                outgoing.stack.push(v1);
            }
        }

        return outgoing;
    }

    private Status parse_POP(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode POP");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot pop from empty stack");
        }
        final Status outgoing = incoming.copy();
        outgoing.stack.pop();
        return outgoing;
    }

    private Status parse_POP2(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode POP2");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot pop from empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        if (isCategory2(v.type)) {
            // Form 2
            return outgoing;
        }
        // Form 1
        if (outgoing.stack.isEmpty()) {
            illegalState("Another type 1 entry is required on stack to pop!");
        }
        outgoing.stack.pop();
        return outgoing;
    }

    private Status parse_SWAP(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode POP");
        if (incoming.stack.size() < 2) {
            illegalState("Expected at least two values on stack for swap");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        final Value b = outgoing.stack.pop();
        outgoing.stack.push(a);
        outgoing.stack.push(b);
        return outgoing;
    }

    private Status parse_ADD_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for addition");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for addition");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for addition");
        }
        final Add add = new Add(desc, b, a);
        outgoing.stack.push(add);
        return outgoing;
    }

    private Status parse_SUB_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for substraction");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for substraction");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for substraction");
        }
        final Sub sub = new Sub(desc, b, a);
        outgoing.stack.push(sub);
        return outgoing;
    }

    private Status parse_MUL_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for multiplication");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + a + " for multiplication");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot add non " + desc + " value " + b + " for multiplication");
        }
        final Mul mul = new Mul(desc, b, a);
        outgoing.stack.push(mul);
        return outgoing;
    }

    private Status parse_ARRAYLENGTH(final OperatorInstruction node, final Status incoming) {
        System.out.println("  opcode ARRAYLENGTH");
        if (incoming.stack.isEmpty()) {
            illegalState("Array on stack is required!");
        }
        final Status outgoing = incoming.copy();
        final Value array = outgoing.stack.pop();
        outgoing.stack.push(new ArrayLength(array));
        return outgoing;
    }

    private Status parse_NEG_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Need a minium of one value on stack for negation");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot negate non " + desc + " value " + a + " of type " + a.type);
        }
        outgoing.stack.push(new Negate(desc, a));
        return outgoing;
    }

    private Status parse_DIV_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for division");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for division");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for division");
        }
        final Div div = new Div(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(div, ControlType.FORWARD);
        outgoing.stack.push(div);
        return outgoing;
    }

    private Status parse_REM_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for remainder");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for remainder");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for remainder");
        }
        final Rem rem = new Rem(desc, b, a);
        outgoing.control = outgoing.control.controlFlowsTo(rem, ControlType.FORWARD);
        outgoing.stack.push(rem);
        return outgoing;
    }

    private Status parse_BITOPERATION_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc, final BitOperation.Operation operation) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for bit operation");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + a + " for bit operation");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            illegalState("Cannot use non " + desc + " value " + b + " for bit operation");
        }
        final BitOperation rem = new BitOperation(desc, operation, b, a);
        outgoing.stack.push(rem);
        return outgoing;
    }

    private Status parse_NUMERICCOMPARE_X(final OperatorInstruction node, final Status incoming, final NumericCompare.Mode mode) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Need a minium of two values on stack for numeric comparison");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        final Value b = outgoing.stack.pop();
        final NumericCompare compare = new NumericCompare(mode, b, a);
        outgoing.stack.push(compare);
        return outgoing;
    }

    private Status parse_MONITORENTER(final MonitorInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode MONITORENTER");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorEnter(v), ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_MONITOREXIT(final MonitorInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode MONITOREXIT");
        if (incoming.stack.isEmpty()) {
            illegalState("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        outgoing.control = outgoing.control.controlFlowsTo(new MonitorExit(v), ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_ASTORE_X(final ArrayStoreInstruction node, final Status incoming, final ClassDesc arrayType) {
        System.out.println("  " + node + " opcode " + node.opcode());
        if (incoming.stack.size() < 3) {
            illegalState("Three stack entries required for array store");
        }
        final Status outgoing = incoming.copy();
        final Value value = outgoing.stack.pop();
        final Value index = outgoing.stack.pop();
        final Value array = outgoing.stack.pop();
        outgoing.memory = outgoing.memory.memoryFlowsTo(new ArrayStore(arrayType, array, index, value));
        return outgoing;
    }

    private Status parse_AASTORE(final ArrayStoreInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode " + node.opcode());
        if (incoming.stack.size() < 3) {
            illegalState("Three stack entries required for array store");
        }
        final Status outgoing = incoming.copy();
        final Value value = outgoing.stack.pop();
        final Value index = outgoing.stack.pop();
        final Value array = outgoing.stack.pop();
        outgoing.control = outgoing.memory.memoryFlowsTo(new ArrayStore(array.type.componentType(), array, index, value));
        return outgoing;
    }

    private Status parse_ALOAD_X(final ArrayLoadInstruction node, final Status incoming, final ClassDesc arrayType, final ClassDesc elementType) {
        System.out.println("  " + node + " opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Two stack entries required for array store");
        }
        final Status outgoing = incoming.copy();
        final Value index = outgoing.stack.pop();
        final Value array = outgoing.stack.pop();
        final Value value = new ArrayLoad(elementType, arrayType, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.stack.push(value);
        return outgoing;
    }

    private Status parse_AALOAD(final ArrayLoadInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            illegalState("Two stack entries required for array store");
        }
        final Status outgoing = incoming.copy();
        final Value index = outgoing.stack.pop();
        final Value array = outgoing.stack.pop();
        final Value value = new ArrayLoad(array.type.componentType(), array.type, array, index);
        outgoing.memory = outgoing.memory.memoryFlowsTo(value);
        outgoing.stack.push(value);
        return outgoing;
    }

    private Status parse_CONVERT_X(final ConvertInstruction node, final Status incoming, final ClassDesc from, final ClassDesc to) {
        System.out.println("  " + node + " opcode " + node.opcode());
        if (incoming.stack.isEmpty()) {
            illegalState("Expected an entry on the stack for type conversion");
        }
        final Status outgoing = incoming.copy();
        final Value value = outgoing.stack.pop();
        if (!value.type.equals(from)) {
            illegalState("Expected a value of type " + from + " but got " + value.type);
        }
        outgoing.stack.push(new Convert(to, value, from));
        return outgoing;
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

        Status copyWithPHI(final LabelNode definer) {
            final Status result = new Status();
            result.lineNumber = lineNumber;
            result.locals = new Value[locals.length];
            for (int i = 0; i < locals.length; i++) {
                if (locals[i] != null) {
                    result.locals[i] = definer.definePHI(locals[i].type);
                }
            }
            result.stack = new Stack<>();
            for (final Value v : stack) {
                result.stack.add(definer.definePHI(v.type));
            }
            result.control = control;
            result.memory = memory;
            return result;
        }
    }

    static class InterpretTask {
        int eleementIndex;
        Status status;
        ControlFlowCondition condition = ControlFlowConditionDefault.INSTANCE;
    }
}
