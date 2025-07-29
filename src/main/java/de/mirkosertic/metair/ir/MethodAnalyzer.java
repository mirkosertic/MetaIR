package de.mirkosertic.metair.ir;

import java.lang.classfile.*;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.instruction.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.*;

public class MethodAnalyzer {

    static class Status {
        int lineNumber = -1;
        Value[] locals;
        Stack<Value> stack;
        Node node;

        Status copy() {
            final Status result = new Status();
            result.lineNumber = lineNumber;
            result.locals = new Value[locals.length];
            System.arraycopy(locals, 0, result.locals, 0, locals.length);
            result.stack = new Stack<>();
            result.stack.addAll(stack);
            result.node = node;
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
            result.node = node;
            return result;
        }
    }


    private final ClassDesc owner;
    private final MethodModel method;
    private final Map<Label, List<CodeElement>> predecessors;
    private final Map<Label, Status> incomingStatus;
    private final Method ir;

    public MethodAnalyzer(final ClassDesc owner, final MethodModel method) {

        this.owner = owner;
        this.predecessors = new HashMap<>();
        this.method = method;
        this.ir = new Method(method);
        this.incomingStatus = new HashMap<>();

        final Optional<CodeModel> optCode = method.code();
        if (optCode.isPresent()) {
            final CodeModel code = optCode.get();
            step1AnalyzeCFG(code);
            step2FollowCFGAndInterpret(code);
            step3MarkBackEdges();
            //step4PeepholeOptimizations();
        }
    }

    public Method ir() {
        return ir;
    }

    private void step1AnalyzeCFG(final CodeModel code) {
        CodeElement prev = null;
        for (final CodeElement elem : code.elementList()) {

            if (elem instanceof final PseudoInstruction psi) {
                if (psi instanceof final LabelTarget node) {
                    final List<CodeElement> preds = predecessors.computeIfAbsent(node.label(), key -> new ArrayList<>());
                    if (prev != null) {
                        preds.add(prev);
                    }
                }
                prev = psi;
            } else if (elem instanceof final Instruction insn) {
                if (insn instanceof final BranchInstruction branch) {
                    final List<CodeElement> preds = predecessors.computeIfAbsent(branch.target(), key -> new ArrayList<>());
                    preds.add(branch);
                    if (branch.opcode() == Opcode.GOTO) {
                        prev = null;
                    } else {
                        prev = branch;
                    }
                } else {
                    switch (insn.opcode()) {
                        case Opcode.IRETURN:
                        case Opcode.ARETURN:
                        case Opcode.FRETURN:
                        case Opcode.LRETURN:
                        case Opcode.DRETURN:
                        case Opcode.RETURN: {
                            prev = null;
                            break;
                        }
                        default: {
                            prev = insn;
                        }
                    }
                }
            }
        }
    }

    static class InterpretTask {
        int eleementIndex;
        Status status;
        ControlFlowCondition condition = ControlFlowConditionDefault.INSTANCE;
    }

    private Status incomingStatusFor(final Status status, final Label label) {
        return incomingStatus.computeIfAbsent(label, key -> {
            final List<CodeElement> preds = predecessors.get(key);
            if (preds == null || preds.isEmpty()) {
                throw new IllegalStateException("No predecessor for " + key);
            }
            if (preds.size() > 1) {
                // PHI Values
                return status.copyWithPHI(ir.createLabel(key));
            }
            return status.copy();
        });
    }

    boolean needsTwoSlots(final ClassDesc type) {
        return type.equals(ConstantDescs.CD_long) || type.equals(ConstantDescs.CD_double);
    }

    private void step2FollowCFGAndInterpret(final CodeModel code) {

        final Deque<InterpretTask> tasks = new ArrayDeque<>();
        final CodeElement start = code.elementList().getFirst();

        final CodeAttribute cm = (CodeAttribute) code;

        final Status initStatus = new Status();
        initStatus.locals = new Value[cm.maxLocals()];
        initStatus.stack = new Stack<>();
        initStatus.node = ir;

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
                    final List<CodeElement> preds = predecessors.get(labelnode.label());
                    if (preds != null && preds.size() > 1) {
                        // TODO: What about the stack here?
                        for (int i = 0; i < newTask.status.locals.length; i++) {
                            final Value v = newTask.status.locals[i];
                            if (v != null && v != status.locals[i]) {
                                //final Copy next = new Copy(status.locals[i], v);
                                v.use(status.locals[i], new PHIUse(status.node));
                            }
                        }
                    }

                    // Keep control flow
                    final LabelNode next = ir.createLabel(labelnode.label());
                    newTask.status.node = status.node.controlFlowsTo(next, ControlType.FORWARD, task.condition);
                    task.condition = ControlFlowConditionDefault.INSTANCE;

                    tasks.push(newTask);

                    break;

                } else if (current instanceof final LabelTarget labelTarget && current == start) {

                    final LabelNode next = ir.createLabel(labelTarget.label());
                    status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);

                }
                System.out.print("#");
                System.out.print(allElements.indexOf(current));
                System.out.print(" : ");

                System.out.println(current);

                System.out.print("[Line ");
                System.out.print(status.lineNumber);

                for (int i = 0; i <status.locals.length; i++) {
                    if (status.locals[i] != null) {
                        System.out.print(" Local ");
                        System.out.print(i);
                        System.out.print("=");
                        System.out.print(status.locals[i]);
                    }
                }

                for (int i = 0; i <status.stack.size(); i++) {
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
                            case Opcode.IF_ICMPGE:
                            case Opcode.IF_ICMPLE: {
                                newTask.condition = ControlFlowConditionOnTrue.INSTANCE;
                                break;
                            }
                        }
                        tasks.push(newTask);
                    }
                    if (jumpInsnNode.opcode() == Opcode.GOTO) {
                        // Unconditional jump and a back edge, we stop analysis here
                        final Status target = incomingStatus.get(jumpInsnNode.target());
                        if (target == null) {
                            throw new IllegalStateException("Unconditional jump to " + jumpInsnNode.target() + " without a target status");
                        }
                        // Do some sanity checking here
                        for (int i = 0; i < status.locals.length; i++) {
                            final Value source = status.locals[i];
                            final Value dest = target.locals[i];
                            if (dest != null && dest != source) {
                                throw new IllegalStateException("Missing copy for local index " + i + ", got " + source + " and expected " + dest);
                            }
                        }
                        break;
                    }
                }
                if (current instanceof final ReturnInstruction returnInstruction) {
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

                if (visited.contains(elementIndex+1)) {
                    break;
                }
            }
            System.out.println("Basic block finished");
        }
    }

    private void step3MarkBackEdges() {
        ir.markBackEdges();
    }

    private void step4PeepholeOptimizations() {
        ir.peepholeOptimizations();
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
                case final TypeCheckInstruction typeCheckInstruction -> parse_CHECKCAST(typeCheckInstruction, incoming);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction, incoming);
                case final OperatorInstruction operatorInstruction ->
                        visitOperatorInstruction(operatorInstruction, incoming);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            };
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
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
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitStackInstruction(final StackInstruction ins, final Status incominmg) {
        return switch (ins.opcode()) {
            case Opcode.DUP -> parse_DUP(ins, incominmg);
            case Opcode.POP -> parse_POP(ins, incominmg);
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

    private Status visitNewObjectInstruction(final NewObjectInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.NEW -> parse_NEW(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitFieldInstruction(final FieldInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case GETFIELD -> parse_GETFIELD(ins, incoming);
            case GETSTATIC -> parse_GETSTATIC(ins, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        };
    }

    private Status visitConstantInstruction(final ConstantInstruction ins, final Status incoming) {
        return switch (ins.opcode()) {
            case Opcode.LDC -> parse_LDC(ins, incoming);
            case Opcode.LDC2_W -> parse_LDC(ins, incoming);
            case Opcode.ICONST_M1, Opcode.ICONST_0, Opcode.ICONST_5, Opcode.ICONST_4, Opcode.ICONST_3, Opcode.ICONST_2, Opcode.ICONST_1 -> parse_ICONST(ins, incoming);
            case Opcode.LCONST_0, Opcode.LCONST_1 -> parse_LCONST(ins, incoming);
            case Opcode.FCONST_0, Opcode.FCONST_1, Opcode.FCONST_2 -> parse_FCONST(ins, incoming);
            case Opcode.DCONST_0, Opcode.DCONST_1 -> parse_DCONST(ins, incoming);
            case Opcode.BIPUSH -> parse_BIPUSH(ins, incoming);
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
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
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

    private Status parse_NEW(final NewObjectInstruction node, final Status incoming) {
        System.out.println("  opcode NEW Creating " + node.className().asSymbol());
        final ClassDesc type = node.className().asSymbol();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = incoming.copy();

        outgoing.node = outgoing.node.controlFlowsTo(init, ControlType.FORWARD);
        outgoing.stack.push(new New(init));

        return outgoing;
    }

    private Status parse_CHECKCAST(final TypeCheckInstruction node, final Status incoming) {
        System.out.println("  opcode CHECKCAST Checking for " + node.type().asSymbol());
        return incoming;
    }

    private Status parse_IINC(final IncrementInstruction node, final Status incoming) {
        // A node that represents an IINC instruction.
        System.out.println("  opcode IINC Local " + node.slot() + " Increment " + node.constant());

        final Status outgoing = incoming.copy();
        outgoing.locals[node.slot()] = new Add(ConstantDescs.CD_int, incoming.locals[node.slot()], ir.definePrimitiveInt(node.constant()));
        return outgoing;
    }

    private Status parse_IF_ICMP_OP(final BranchInstruction node, final Status incoming, final Compare.Operation op) {
        System.out.println("  opcode IFCMP_GE Target " + node.target());
        if (incoming.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for comparison");
        }

        final Status outgoing = incoming.copy();

        final Value v1 = outgoing.stack.pop();
        final Value v2 = outgoing.stack.pop();

        final Compare compare = new Compare(op, v1, v2);
        final If next = new If(compare);
        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);
        // TODO: Handle jumps and copy of phi?
        return outgoing;
    }

    private Status parse_IFEQ(final BranchInstruction node, final Status incoming) {
        System.out.println("  opcode IFEQ Target " + node.target());
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Need a value on stack for comparison");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();

        final Compare compare = new Compare(Compare.Operation.EQ, v, ir.definePrimitiveInt(0));
        final If next = new If(compare);

        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_GOTO(final BranchInstruction node, final Status incoming) {
        System.out.println("  opcode GOTO Target " + node.target());

        final LabelNode label = ir.createLabel(node.target());

        final Status targetIncoming = incomingStatusFor(incoming, node.target());

        final Status outgoing = incoming.copy();

        final Goto next = new Goto();
        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);

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

    private Status visitBranchInstruction(final BranchInstruction node, final Status incoming) {
        // A node that represents a jump instruction. A jump instruction is an instruction that may jump to another instruction.
        return switch (node.opcode()) {
            case Opcode.IF_ICMPGE -> parse_IF_ICMP_OP(node, incoming, Compare.Operation.GE);
            case Opcode.IF_ICMPLE -> parse_IF_ICMP_OP(node, incoming, Compare.Operation.LE);
            case Opcode.IFEQ -> parse_IFEQ(node, incoming);
            case Opcode.GOTO -> parse_GOTO(node, incoming);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status parse_BIPUSH(final ConstantInstruction node, final Status incoming) {
        System.out.println("  opcode " + node.opcode());
        final Status outgoing = incoming.copy();
        outgoing.stack.push(ir.definePrimitiveByte((Integer) node.constantValue()));
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

    private Status parse_GETSTATIC(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode GETSTATIC Field " + node.field());

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        final Status outgoing = incoming.copy();

        outgoing.node = outgoing.node.controlFlowsTo(init, ControlType.FORWARD);

        final GetStaticField get = new GetStaticField(node, ri);
        outgoing.stack.push(get);

        outgoing.node = outgoing.node.controlFlowsTo(get, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_GETFIELD(final FieldInstruction node, final Status incoming) {
        System.out.println("  opcode GETFIELD Field " + node.field());
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot load field from empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        if (v.type.isPrimitive() || v.type.isArray()) {
            throw new IllegalStateException("Cannot load field from non object value " + v);
        }
        final GetInstanceField get = new GetInstanceField(node, v);
        outgoing.stack.push(get);

        outgoing.node = outgoing.node.controlFlowsTo(get, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_RETURN(final ReturnInstruction node, final Status incoming) {
        System.out.println("  opcode RETURN");

        final Return next = new Return();

        final Status outgoing = incoming.copy();
        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);
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

    private Status parse_ADD_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for addition");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + a + " for addition");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + b + " for addition");
        }
        final Add add = new Add(desc, b, a);
        outgoing.stack.push(add);
        return outgoing;
    }

    private Status parse_SUB_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for substraction");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + a + " for substraction");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + b + " for substraction");
        }
        final Sub sub = new Sub(desc, b, a);
        outgoing.stack.push(sub);
        return outgoing;
    }

    private Status parse_MUL_X(final OperatorInstruction node, final Status incoming, final ClassDesc desc) {
        System.out.println("  opcode " + node.opcode());
        if (incoming.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for multiplication");
        }
        final Status outgoing = incoming.copy();
        final Value a = outgoing.stack.pop();
        if (!a.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + a + " for multiplication");
        }
        final Value b = outgoing.stack.pop();
        if (!b.type.equals(desc)) {
            throw new IllegalStateException("Cannot add non " + desc + " value " + b + " for multiplication");
        }
        final Mul mul = new Mul(desc, b, a);
        outgoing.stack.push(mul);
        return outgoing;
    }

    private Status parse_ARETURN(final ReturnInstruction node, final Status incoming) {
        System.out.println("  opcode ARETURN");
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot return empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();

        final MethodTypeDesc methodTypeDesc = method.methodTypeSymbol();
        if (!v.type.equals(methodTypeDesc.returnType())) {
            throw new IllegalStateException("Expecting type " + methodTypeDesc.returnType() + " on stack, got " + v.type);
        }

        final ReturnValue next = new ReturnValue(v.type, v);
        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_RETURN_X(final ReturnInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode RETURN");
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot return empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.pop();
        if (!v.type.equals(type)) {
            throw new IllegalStateException("Cannot return non " + type + " value " + v);
        }
        final ReturnValue next = new ReturnValue(type, v);
        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);
        return outgoing;
    }

    private Status parse_DUP(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode DUP");
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        final Value v = outgoing.stack.peek();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_POP(final StackInstruction node, final Status incoming) {
        System.out.println("  " + node + " opcode POP");
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot duplicate empty stack");
        }
        final Status outgoing = incoming.copy();
        outgoing.stack.pop();
        return outgoing;
    }

    private Status parse_ALOAD(final LoadInstruction node, final Status incoming) {
        System.out.println("  opcode ALOAD Local " + node.slot());
        final Value v = incoming.locals[node.slot()];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.slot());
        }

        final Status outgoing = incoming.copy();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_LOAD_TYPE(final LoadInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode ILOAD Local " + node.slot());
        final Value v = incoming.locals[node.slot()];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.slot());
        }
        if (!v.type.equals(type)) {
            throw new IllegalStateException("Cannot load non " + type + " value " + v + " for index " + node.slot());
        }
        final Status outgoing = incoming.copy();
        outgoing.stack.push(v);
        return outgoing;
    }

    private Status parse_STORE_TYPE(final StoreInstruction node, final Status incoming, final ClassDesc type) {
        System.out.println("  opcode ISTORE Local " + node.slot());
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
        }
        final Status outgoing = incoming.copy();

        final Value v = outgoing.stack.pop();
        if (!v.type.equals(type)) {
            throw new IllegalStateException("Cannot store non " + type + " value " + v + " for index " + node.slot());
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

    private Status parse_ASTORE(final StoreInstruction node, final Status incoming) {
        System.out.println("  opcode ASTORE Local " + node.slot());
        if (incoming.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
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

    private Status visitLoadInstruction(final LoadInstruction node, final Status incoming) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        return switch (node.opcode()) {
            case Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_2, Opcode.ALOAD_1, Opcode.ALOAD_0 -> parse_ALOAD(node, incoming);
            case Opcode.ILOAD, Opcode.ILOAD_3, Opcode.ILOAD_2, Opcode.ILOAD_1, Opcode.ILOAD_0 -> parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_int);
            case Opcode.DLOAD, Opcode.DLOAD_3, Opcode.DLOAD_2, Opcode.DLOAD_1, Opcode.DLOAD_0 -> parse_LOAD_TYPE(node, incoming, ConstantDescs.CD_double);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status visitStoreInstruction(final StoreInstruction node, final Status incoming) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        return switch (node.opcode()) {
            case Opcode.ASTORE, Opcode.ASTORE_3, Opcode.ASTORE_2, Opcode.ASTORE_1, Opcode.ASTORE_0 -> parse_ASTORE(node, incoming);
            case Opcode.ISTORE, Opcode.ISTORE_3, Opcode.ISTORE_2, Opcode.ISTORE_1, Opcode.ISTORE_0 -> parse_STORE_TYPE(node, incoming, ConstantDescs.CD_int);
            case Opcode.LSTORE, Opcode.LSTORE_3, Opcode.LSTORE_2, Opcode.LSTORE_1, Opcode.LSTORE_0 -> parse_STORE_TYPE(node, incoming, ConstantDescs.CD_long);
            default -> throw new IllegalArgumentException("Not implemented yet : " + node);
        };
    }

    private Status parse_INVOKESPECIAL(final InvokeInstruction node, final Status incoming) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        final Status outgoing = incoming.copy();

        System.out.println("  opcode INVOKESPECIAL Method " + node.name() + " desc " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (outgoing.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(outgoing.stack.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        outgoing.node = outgoing.node.controlFlowsTo(next, ControlType.FORWARD);

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
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
        } else {
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
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
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }

        final Status outgoing = incoming.copy();

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
        } else {
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
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
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + incoming.stack.size());
        }

        final Status outgoing = incoming.copy();

        final RuntimeclassReference runtimeClass = ir.defineRuntimeclassReference(node.method().owner().asSymbol());

        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = outgoing.stack.pop();
            arguments.add(v);
        }
        arguments.add(runtimeClass);

        final ClassInitialization init = new ClassInitialization(runtimeClass);

        outgoing.node = outgoing.node.controlFlowsTo(init, ControlType.FORWARD);

        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            outgoing.stack.push(invocation);
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
        } else {
            outgoing.node = outgoing.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
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

    private Status visitLabelTarget(final LabelTarget node, final Status incoming) {
        // An AbstractInsnNode that encapsulates a Label.
        System.out.print("  Label: " + node.label());
        final List<CodeElement> preds = predecessors.get(node.label());
        if (preds != null) {
            System.out.print(" Jumped from [");
            for (int i = 0; i < preds.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(preds.get(i));
            }
            System.out.print("]");
        }
        System.out.println();

        return incoming;
    }

    private Status visitLineNumberNode(final LineNumber node, final Status incoming) {
        // A node that represents a line number declaration. These nodes are pseudo instruction nodes in order to be inserted in an instruction list.
        final Status n = incoming.copy();
        n.lineNumber = node.line();
        System.out.println("  Line " + node.line());
        return n;
    }
}
