package de.mirkosertic.metair.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
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

        Status copyWithPHI(final Label definer) {
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


    private final Type owner;
    private final MethodNode method;
    private final Map<LabelNode, List<AbstractInsnNode>> predecessors;
    private final Map<LabelNode, Status> incomingStatus;
    private final Method ir;

    public MethodAnalyzer(final Type owner, final MethodNode method) {

        this.owner = owner;
        this.predecessors = new HashMap<>();
        this.method = method;
        this.ir = new Method(method);
        this.incomingStatus = new HashMap<>();

        step1AnalyzeCFG(method);
        step2FollowCFGAndInterpret(method);
        step3MarkBackEdges();
        //step4PeepholeOptimizations();
    }

    public Method ir() {
        return ir;
    }

    private void step1AnalyzeCFG(final MethodNode method) {
        AbstractInsnNode prev = null;
        for (final AbstractInsnNode insn : method.instructions) {

            if (ignoreNode(insn)) {
                continue;
            }

            if (insn instanceof final LabelNode node) {
                final List<AbstractInsnNode> preds = predecessors.computeIfAbsent(node, key -> new ArrayList<>());
                if (prev != null) {
                    preds.add(prev);
                }
                prev = insn;
            } else if (insn instanceof final JumpInsnNode node) {
                final List<AbstractInsnNode> preds = predecessors.computeIfAbsent(node.label, key -> new ArrayList<>());
                preds.add(node);
                if (node.getOpcode() == Opcodes.GOTO) {
                    prev = null;
                } else {
                    prev = node;
                }
            } else {
                switch (insn.getOpcode()) {
                    case Opcodes.IRETURN:
                    case Opcodes.ARETURN:
                    case Opcodes.FRETURN:
                    case Opcodes.LRETURN:
                    case Opcodes.DRETURN:
                    case Opcodes.RETURN: {
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

    private boolean ignoreNode(final AbstractInsnNode node) {
        if (node instanceof LabelNode && node.getNext() == null) {
            // Ignore last label in instruction list
            return true;
        }
        return false;
    }

    static class InterpretTask {
        AbstractInsnNode insn;
        Status status;
        ControlFlowCondition condition = ControlFlowConditionDefault.INSTANCE;
    }

    private Status incomingStatusFor(final Status status, final LabelNode targetLabel) {
        return incomingStatus.computeIfAbsent(targetLabel, key -> {
            final List<AbstractInsnNode> preds = predecessors.get(targetLabel);
            if (preds == null || preds.isEmpty()) {
                throw new IllegalStateException("No predecessor for " + targetLabel);
            }
            if (preds.size() > 1) {
                // PHI Values
                return status.copyWithPHI(ir.createLabel(targetLabel));
            }
            return status.copy();
        });
    }

    private void step2FollowCFGAndInterpret(final MethodNode method) {

        final Deque<InterpretTask> tasks = new ArrayDeque<>();
        final AbstractInsnNode start = method.instructions.getFirst();

        final Status initStatus = new Status();
        initStatus.locals = new Value[method.maxLocals];
        initStatus.stack = new Stack<>();
        initStatus.node = ir;

        // Locals anhand der Methodensignatur vorinitialisieren
        int localIndex = 0;
        if (!Modifier.isStatic(method.access)) {
            initStatus.locals[localIndex++] = ir.defineThisRef(owner);
        }
        final Type[] argumentTypes = Type.getArgumentTypes(method.desc);
        for (int i = 0; i < argumentTypes.length; i++) {
            initStatus.locals[localIndex++] = ir.defineMethodArgument(argumentTypes[i], i);
            if (argumentTypes[i].getSize() == 2) {
                initStatus.locals[localIndex++] = null;
            }
        }

        final InterpretTask initialTask = new InterpretTask();
        initialTask.insn = start;
        initialTask.status = initStatus;
        tasks.push(initialTask);

        final Set<AbstractInsnNode> visited = new HashSet<>();
        visited.add(start);

        while (!tasks.isEmpty()) {
            final InterpretTask task = tasks.pop();
            AbstractInsnNode current = task.insn;
            final Status status = task.status;

            System.out.print("Analyzing basic block at #");
            System.out.print(method.instructions.indexOf(current));
            System.out.println();

            while (current != null) {
                if (!ignoreNode(current)) {
                    if (current instanceof LabelNode && !visited.contains(current)) {
                        visited.add(current);
                        final InterpretTask newTask = new InterpretTask();
                        newTask.insn = current;
                        newTask.status = incomingStatusFor(status, (LabelNode) current);

                        Node startFlow = status.node;

                        // Create PHI assignments / initializations here...
                        final List<AbstractInsnNode> preds = predecessors.get(current);
                        if (preds != null && preds.size() > 1) {
                            // TODO: What about the stack here?
                            for (int i = 0; i < newTask.status.locals.length; i++) {
                                final Value v = newTask.status.locals[i];
                                if (v != null && v != status.locals[i]) {
                                    final Copy next = new Copy(status.locals[i], v);
                                    startFlow = startFlow.controlFlowsTo(next, ControlType.FORWARD);
                                }
                            }
                        }

                        // Keep control flow
                        final Label next = ir.createLabel(current);
                        newTask.status.node = startFlow.controlFlowsTo(next, ControlType.FORWARD, task.condition);
                        task.condition = ControlFlowConditionDefault.INSTANCE;

                        tasks.push(newTask);

                        break;

                    } else if (current instanceof LabelNode && current == start) {

                        final Label next = ir.createLabel(current);
                        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);

                    }
                    System.out.print("#");
                    System.out.print(method.instructions.indexOf(current));
                    System.out.print(" : ");

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

                    visitNode(current, status);

                    visited.add(current);

                    if (current instanceof final JumpInsnNode jumpInsnNode) {
                        if (!visited.contains(jumpInsnNode.label)) {
                            final InterpretTask newTask = new InterpretTask();
                            newTask.insn = jumpInsnNode.label;
                            newTask.status = incomingStatusFor(status, jumpInsnNode.label);
                            switch (current.getOpcode()) {
                                case Opcodes.IFEQ:
                                case Opcodes.IF_ICMPGE: {
                                    newTask.condition = ControlFlowConditionOnTrue.INSTANCE;
                                    break;
                                }
                            }
                            tasks.push(newTask);
                        }
                        if (jumpInsnNode.getOpcode() == Opcodes.GOTO) {
                            // Unconditional jump
                            break;
                        }
                    }
                    if (current instanceof InsnNode) {
                        if (current.getOpcode() == Opcodes.RETURN) {
                            break;
                        }
                        if (current.getOpcode() == Opcodes.IRETURN) {
                            break;
                        }
                        if (current.getOpcode() == Opcodes.LRETURN) {
                            break;
                        }
                        if (current.getOpcode() == Opcodes.FRETURN) {
                            break;
                        }
                        if (current.getOpcode() == Opcodes.DRETURN) {
                            break;
                        }
                        if (current.getOpcode() == Opcodes.ARETURN) {
                            break;
                        }
                    }
                }
                current = current.getNext();
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

    public static String opcodeToInstruction(final int opcode) {
        if (opcode == Opcodes.ALOAD) {
            return "ALOAD";
        }
        if (opcode == Opcodes.INVOKESPECIAL) {
            return "INVOKESPECIAL";
        }
        if (opcode == Opcodes.RETURN) {
            return "RETURN";
        }
        if (opcode == Opcodes.GETSTATIC) {
            return "GETSTATIC";
        }
        if (opcode == Opcodes.LDC) {
            return "LDC";
        }
        if (opcode == Opcodes.INVOKEVIRTUAL) {
            return "INVOKEVIRTUAL";
        }
        if (opcode == Opcodes.ISTORE) {
            return "ISTORE";
        }
        if (opcode == Opcodes.ILOAD) {
            return "ILOAD";
        }
        if (opcode == Opcodes.BIPUSH) {
            return "BIPUSH";
        }
        if (opcode == Opcodes.IADD) {
            return "IADD";
        }
        if (opcode == Opcodes.IINC) {
            return "IINC";
        }
        if (opcode == Opcodes.IRETURN) {
            return "IRETURN";
        }
        if (opcode == Opcodes.ARETURN) {
            return "ARETURN";
        }
        if (opcode == Opcodes.SIPUSH) {
            return "SIPUSH";
        }
        if (opcode == Opcodes.ICONST_0) {
            return "ICONST_0";
        }
        if (opcode == Opcodes.ICONST_1) {
            return "ICONST_1";
        }
        if (opcode == Opcodes.ICONST_2) {
            return "ICONST_2";
        }
        if (opcode == Opcodes.ICONST_3) {
            return "ICONST_3";
        }
        if (opcode == Opcodes.ICONST_4) {
            return "ICONST_4";
        }
        if (opcode == Opcodes.ICONST_5) {
            return "ICONST_5";
        }
        if (opcode == Opcodes.NEW) {
            return "NEW";
        }
        if (opcode == Opcodes.DUP) {
            return "DUP";
        }
        if (opcode == Opcodes.INVOKEDYNAMIC) {
            return "INVOKEDYNAMIC";
        }
        if (opcode == Opcodes.ATHROW) {
            return "ATHROW";
        }
        if (opcode == Opcodes.ASTORE) {
            return "ASTORE";
        }
        if (opcode == Opcodes.GETFIELD) {
            return "GETFIELD";
        }
        if (opcode == Opcodes.INVOKEINTERFACE) {
            return "INVOKEINTERFACE";
        }
        if (opcode == Opcodes.CHECKCAST) {
            return "CHECKCAST";
        }
        if (opcode == Opcodes.INSTANCEOF) {
            return "INSTANCEOF";
        }
        if (opcode == Opcodes.INVOKESTATIC) {
            return "INVOKESTATIC";
        }
        if (opcode == Opcodes.IF_ICMPGE) {
            return "IF_ICMPGE";
        }
        if (opcode == Opcodes.GOTO) {
            return "GOTO";
        }
        if (opcode == Opcodes.IFEQ) {
            return "IFEQ";
        }
        throw new IllegalArgumentException("Not implemented yet : " + opcode);
    }

    private void visitNode(final AbstractInsnNode node, final Status status) {

        if (node instanceof FrameNode) {
            visitFrameNode((FrameNode) node, status);
        } else if (node instanceof LabelNode) {
            visitLabelNode((LabelNode) node, status);
        } else if (node instanceof LineNumberNode) {
            visitLineNumberNode((LineNumberNode) node, status);
        } else if (node instanceof VarInsnNode) {
            visitVarInsNode((VarInsnNode) node, status);
        } else if (node instanceof MethodInsnNode) {
            visitMethodInsNode((MethodInsnNode) node, status);
        } else if (node instanceof InsnNode) {
            visitInsNode((InsnNode) node, status);
        } else if (node instanceof FieldInsnNode) {
            visitFieldInsNode((FieldInsnNode) node, status);
        } else if (node instanceof LdcInsnNode) {
            parse_LDC((LdcInsnNode) node, status);
        } else if (node instanceof IntInsnNode) {
            visitIntInsNode((IntInsnNode) node, status);
        } else if (node instanceof JumpInsnNode) {
            visitJumpInsNode((JumpInsnNode) node, status);
        } else if (node instanceof IincInsnNode) {
            parse_IINC((IincInsnNode) node, status);
        } else if (node instanceof TypeInsnNode) {
            visitTypeInsnNode((TypeInsnNode) node, status);
        } else if (node instanceof InvokeDynamicInsnNode) {
            parse_INVOKEDYNAMIC((InvokeDynamicInsnNode) node, status);
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void parse_INVOKEDYNAMIC(final InvokeDynamicInsnNode node, final Status status) {
        System.out.println("  opcode INVOKEDYNAMIC Method " + node.name + " " + node.desc);
        final Type returnType = Type.getReturnType(node.desc);
        final Type[] args = Type.getArgumentTypes(node.desc);
        final int expectedarguments = args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name + " " + node.desc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
        }
        if (returnType != Type.VOID_TYPE) {
            final Result r = new Result(returnType);
            // TODO: Create invocation here
            status.stack.push(new Result(returnType));
        }
    }

    private void parse_NEW(final TypeInsnNode node, final Status status) {
        System.out.println("  opcode NEW Creating " + node.desc);
        final Type type = Type.getObjectType(node.desc);

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        status.node = status.node.controlFlowsTo(init, ControlType.FORWARD);

        final Result r = new Result(type);
        final New n = new New(ri);
        final Copy c = new Copy(n, r);

        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_CHECKCAST(final TypeInsnNode node, final Status status) {
        System.out.println("  opcode CHECKCAST Checking for " + node.desc);
    }

    private void visitTypeInsnNode(final TypeInsnNode node, final Status status) {
        // A node that represents a type instruction.
        switch (node.getOpcode()) {
            case Opcodes.NEW: {
                parse_NEW(node, status);
                break;
            }
            case Opcodes.CHECKCAST: {
                parse_CHECKCAST(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void parse_IINC(final IincInsnNode node, final Status status) {
        // A node that represents an IINC instruction.
        System.out.println("  opcode IINC Local " + node.var + " Increment " + node.incr);

        final Result r = new Result(Type.INT_TYPE);
        final Copy c = new Copy(new Add(Type.INT_TYPE, status.locals[node.var], ir.definePrimitiveInt(node.incr)), r);

        status.locals[node.var] = r;
        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_IF_ICMPGE(final JumpInsnNode node, final Status status) {
        System.out.println("  opcode IFCMP_GE Target " + node.label.getLabel());
        if (status.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for comparison");
        }
        final Value v1 = status.stack.pop();
        final Value v2 = status.stack.pop();

        final Compare compare = new Compare(Compare.Operation.GE, v1, v2);
        final If next = new If(compare);
        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
        // TODO: Handle jumps and copy of phi?
    }

    private void parse_IFEQ(final JumpInsnNode node, final Status status) {
        System.out.println("  opcode IFEQ Target " + node.label.getLabel());
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Need a value on stack for comparison");
        }
        final Value v = status.stack.pop();

        final Compare compare = new Compare(Compare.Operation.EQ, v, ir.definePrimitiveInt(0));
        final If next = new If(compare);

        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_GOTO(final JumpInsnNode node, final Status status) {
        System.out.println("  opcode GOTO Target " + node.label.getLabel());

        final Label label = ir.createLabel(node.label);

        final Status incoming = incomingStatusFor(status, node.label);

        Node control = status.node;
        for (int i = 0; i < status.locals.length; i++) {
            final Value v = status.locals[i];
            final Value target = incoming.locals[i];
            if (v != null && v != target) {

                final Copy next = new Copy(v, target);
                control = control.controlFlowsTo(next, ControlType.FORWARD);
            }
        }


        final Goto next = new Goto();
        status.node = control.controlFlowsTo(next, ControlType.FORWARD);

        next.controlFlowsTo(label, ControlType.FORWARD);
    }

    private void visitJumpInsNode(final JumpInsnNode node, final Status status) {
        // A node that represents a jump instruction. A jump instruction is an instruction that may jump to another instruction.
        switch (node.getOpcode()) {
            case Opcodes.IF_ICMPGE: {
                parse_IF_ICMPGE(node, status);
                break;
            }
            case Opcodes.IFEQ: {
                parse_IFEQ(node, status);
                break;
            }
            case Opcodes.GOTO: {
                parse_GOTO(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void parse_BIPUSH(final IntInsnNode node, final Status status) {
        System.out.println("  opcode " + opcodeToInstruction(node.getOpcode()));
        status.stack.push(new PrimitiveByte(node.operand));
    }

    private void visitIntInsNode(final IntInsnNode node, final Status status) {
        // A node that represents an instruction with a single int operand.
        switch (node.getOpcode()) {
            case Opcodes.BIPUSH: {
                parse_BIPUSH(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void parse_LDC(final LdcInsnNode node, final Status status) {
        // A node that represents an LDC instruction.
        System.out.println("  opcode LDC Constant " + node.cst.getClass().getName() + " -> " + node.cst);
        if (node.cst instanceof String) {
            status.stack.push(new StringConstant((String) node.cst));
        } else if (node.cst instanceof Type) {
            status.stack.push(ir.defineRuntimeclassReference((Type) node.cst));
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()) + " " + node.cst.getClass().getName());
        }
    }

    private void parse_GETSTATIC(final FieldInsnNode node, final Status status) {
        System.out.println("  opcode GETSTATIC Field " + node.name + " " + node.desc);

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(Type.getObjectType(node.owner));
        final ClassInitialization init = new ClassInitialization(ri);

        status.node = status.node.controlFlowsTo(init, ControlType.FORWARD);

        final GetStaticField get = new GetStaticField(node, ri);

        final Result r = new Result(get.type);
        final Copy c = new Copy(get, r);
        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_GETFIELD(final FieldInsnNode node, final Status status) {
        System.out.println("  opcode GETFIELD Field " + node.name + " " + node.desc);
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot load field from empty stack");
        }
        final Value v = status.stack.pop();
        if (!(v.type.getSort() == Type.OBJECT)) {
            throw new IllegalStateException("Cannot load field from non object value " + v);
        }
        final GetInstanceField get = new GetInstanceField(node, v);
        final Result r = new Result(get.type);
        final Copy c = new Copy(get, r);
        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void visitFieldInsNode(final FieldInsnNode node, final Status status) {
        // A node that represents a field instruction. A field instruction is an instruction that loads or stores the value of a field of an object.
        switch (node.getOpcode()) {
            case Opcodes.GETSTATIC: {
                parse_GETSTATIC(node, status);
                break;
            }
            case Opcodes.GETFIELD: {
                parse_GETFIELD(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void parse_RETURN(final InsnNode node, final Status status) {
        System.out.println("  opcode RETURN");

        final Return next = new Return();
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_ICONST(final InsnNode node, final int constant, final Status status) {
        System.out.println("  opcode ICONST " + constant);
        status.stack.push(ir.definePrimitiveInt(constant));
    }

    private void parse_IADD(final InsnNode node, final Status status) {
        System.out.println("  opcode IADD");
        if (status.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for addition");
        }
        final Value a = status.stack.pop();
        if (a.type != Type.INT_TYPE) {
            throw new IllegalStateException("Cannot add non int value " + a + " for addition");
        }
        final Value b = status.stack.pop();
        if (b.type != Type.INT_TYPE) {
            throw new IllegalStateException("Cannot add non int value " + b + " for addition");
        }
        final Add add = new Add(Type.INT_TYPE, a, b);
        status.stack.push(add);
    }

    private void parse_IRETURN(final InsnNode node, final Status status) {
        System.out.println("  opcode IRETURN");
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot return empty stack");
        }
        final Value v = status.stack.pop();
        if (v.type != Type.INT_TYPE) {
            throw new IllegalStateException("Cannot return non int value " + v);
        }
        final ReturnValue next = new ReturnValue(Type.INT_TYPE, v);
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_DUP(final InsnNode node, final Status status) {
        System.out.println("  " + node + " opcode DUP");
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot duplicate empty stack");
        }
        final Value v = status.stack.peek();
        status.stack.push(v);
    }

    private void visitInsNode(final InsnNode node, final Status status) {
        // A node that represents a zero operand instruction.
        switch (node.getOpcode()) {
            case Opcodes.RETURN: {
                parse_RETURN(node, status);
                break;
            }
            case Opcodes.DUP: {
                parse_DUP(node, status);
                break;
            }
            case Opcodes.IRETURN: {
                parse_IRETURN(node, status);
                break;
            }
            case Opcodes.IADD: {
                parse_IADD(node, status);
                break;
            }
            case Opcodes.ICONST_0: {
                parse_ICONST(node, 0, status);
                break;
            }
            case Opcodes.ICONST_1: {
                parse_ICONST(node, 1, status);
                break;
            }
            case Opcodes.ICONST_2: {
                parse_ICONST(node, 2, status);
                break;
            }
            case Opcodes.ICONST_3: {
                parse_ICONST(node, 3, status);
                break;
            }
            case Opcodes.ICONST_4: {
                parse_ICONST(node, 4, status);
                break;
            }
            case Opcodes.ICONST_5: {
                parse_ICONST(node, 5, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void parse_ALOAD(final VarInsnNode node, final Status status) {
        System.out.println("  opcode ALOAD Local " + node.var);
        final Value v = status.locals[node.var];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.var);
        }
        status.stack.push(v);
    }

    private void parse_ILOAD(final VarInsnNode node, final Status status) {
        System.out.println("  opcode ILOAD Local " + node.var);
        final Value v = status.locals[node.var];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.var);
        }
        if (!(v.type == Type.INT_TYPE)) {
            throw new IllegalStateException("Cannot load non int value " + v + " for index " + node.var);
        }
        status.stack.push(v);
    }

    private void parse_ISTORE(final VarInsnNode node, final Status status) {
        System.out.println("  opcode ISTORE Local " + node.var);
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
        }
        status.locals[node.var] = status.stack.pop();
        if (node.var > 0) {
            if (status.locals[node.var - 1] != null && status.locals[node.var - 1].type.getSize() == 2) {
                // Remove potential illegal values
                status.locals[node.var - 1] = null;
            }
        }
    }

    private void parse_ASTORE(final VarInsnNode node, final Status status) {
        System.out.println("  opcode ASTORE Local " + node.var);
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
        }
        status.locals[node.var] = status.stack.pop();
        if (node.var > 0) {
            if (status.locals[node.var - 1] != null && status.locals[node.var - 1].type.getSize() == 2) {
                // Remove potential illegal values
                status.locals[node.var - 1] = null;
            }
        }
    }

    private void visitVarInsNode(final VarInsnNode node, final Status status) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (node.getOpcode()) {
            case Opcodes.ALOAD: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcodes.ILOAD: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcodes.ASTORE: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcodes.ISTORE: {
                parse_ISTORE(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
        //System.out.println("  " + node + " opcode " + opcodeToInstruction(node.getOpcode()) + " Local " + node.var);
    }

    private void parse_INVOKESPECIAL(final MethodInsnNode node, final Status status) {
        System.out.println("  opcode INVOKESPECIAL Method " + node.name + " desc " + node.desc);
        final Type returnType = Type.getReturnType(node.desc);
        final Type[] args = Type.getArgumentTypes(node.desc);
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name + " " + node.desc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(status.stack.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);

        if (returnType != Type.VOID_TYPE) {
            status.stack.push(next);
        }
    }

    private void parse_INVOKEVIRTUAL(final MethodInsnNode node, final Status status) {
        System.out.println("  opcode INVOKEVIRTUAL Method " + node.name + " " + node.desc);
        final Type returnType = Type.getReturnType(node.desc);
        final Type[] args = Type.getArgumentTypes(node.desc);
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name + " " + node.desc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (returnType != Type.VOID_TYPE) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void parse_INVOKEINTERFACE(final MethodInsnNode node, final Status status) {
        System.out.println("  opcode INVOKEINTERFACE Method " + node.name + " " + node.desc);
        final Type returnType = Type.getReturnType(node.desc);
        final Type[] args = Type.getArgumentTypes(node.desc);
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name + " " + node.desc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (returnType != Type.VOID_TYPE) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void parse_INVOKESTATIC(final MethodInsnNode node, final Status status) {
        System.out.println("  opcode INVOKESTATIC Method " + node.name + " " + node.desc);
        final Type returnType = Type.getReturnType(node.desc);
        final Type[] args = Type.getArgumentTypes(node.desc);
        final int expectedarguments = args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name + " " + node.desc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (returnType != Type.VOID_TYPE) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void visitMethodInsNode(final MethodInsnNode node, final Status status) {
        // A node that represents a method instruction. A method instruction is an instruction that invokes a method.
        switch (node.getOpcode()) {
            case Opcodes.INVOKESPECIAL: {
                parse_INVOKESPECIAL(node, status);
                break;
            }
            case Opcodes.INVOKEVIRTUAL: {
                parse_INVOKEVIRTUAL(node, status);
                break;
            }
            case Opcodes.INVOKEINTERFACE: {
                parse_INVOKEINTERFACE(node, status);
                break;
            }
            case Opcodes.INVOKESTATIC: {
                parse_INVOKESTATIC(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node + " " + opcodeToInstruction(node.getOpcode()));
            }
        }
    }

    private void visitFrameNode(final FrameNode node, final Status status) {
        System.out.println("  Frame: " + node.type);
        System.out.println("   Locals: " + node.local);
        System.out.println("   Stack : " + node.stack);
    }

    private void visitLabelNode(final LabelNode node, final Status status) {
        // An AbstractInsnNode that encapsulates a Label.
        System.out.print("  Label: " + node.getLabel());
        final List<AbstractInsnNode> preds = predecessors.get(node);
        if (preds != null) {
            System.out.print(" Jumped from [");
            for (int i = 0; i < preds.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(method.instructions.indexOf(preds.get(i)));
            }
            System.out.print("]");
        }
        System.out.println();
    }

    private void visitLineNumberNode(final LineNumberNode node, final Status status) {
        // A node that represents a line number declaration. These nodes are pseudo instruction nodes in order to be inserted in an instruction list.
        status.lineNumber = node.line;
        System.out.println("  Line " + node.line);
    }
}
