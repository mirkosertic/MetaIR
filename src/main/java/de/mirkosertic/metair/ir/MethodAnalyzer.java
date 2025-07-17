package de.mirkosertic.metair.ir;

import java.lang.classfile.*;
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

    private int determineMaximumNumberOfLocals(final CodeModel codeModel) {
        int max = 0;
        if (!method.flags().flags().contains(AccessFlag.STATIC)) {
            max = 1;
        }
        final MethodTypeDesc desc = method.methodTypeSymbol();
        for (final ClassDesc type : desc.parameterArray()) {
            if (type.equals(ConstantDescs.CD_long) || type.equals(ConstantDescs.CD_double)) {
                max+=2;
            } else {
                max++;
            }
        }

        for (final CodeElement elem : codeModel.elementList()) {
            if (elem instanceof final LocalVariable localVariable) {
                int pos = localVariable.slot();
                if (ConstantDescs.CD_long.equals(localVariable.typeSymbol()) || ConstantDescs.CD_double.equals(localVariable.typeSymbol())) {
                    pos++;
                }
                max = Math.max(max, pos);
            } else if (elem instanceof final LoadInstruction load) {
                int pos = load.slot();
                switch (load.opcode()) {
                    case Opcode.DLOAD:
                    case Opcode.DLOAD_0:
                    case Opcode.DLOAD_1:
                    case Opcode.DLOAD_2:
                    case Opcode.DLOAD_3:
                    case Opcode.LLOAD:
                    case Opcode.LLOAD_0:
                    case Opcode.LLOAD_1:
                    case Opcode.LLOAD_2:
                    case Opcode.LLOAD_3: {
                        pos++;
                    }
                }
                max = Math.max(max, pos);
            } else if (elem instanceof final StoreInstruction store) {
                int pos = store.slot();
                switch (store.opcode()) {
                    case Opcode.DSTORE:
                    case Opcode.DSTORE_0:
                    case Opcode.DSTORE_1:
                    case Opcode.DSTORE_2:
                    case Opcode.DSTORE_3:
                    case Opcode.LSTORE:
                    case Opcode.LSTORE_0:
                    case Opcode.LSTORE_1:
                    case Opcode.LSTORE_2:
                    case Opcode.LSTORE_3: {
                        pos++;
                    }
                }
                max = Math.max(max, pos);
            }
        }
        return max + 1;
    }

    private void step2FollowCFGAndInterpret(final CodeModel code) {

        final Deque<InterpretTask> tasks = new ArrayDeque<>();
        final CodeElement start = code.elementList().getFirst();

        final Status initStatus = new Status();
        initStatus.locals = new Value[determineMaximumNumberOfLocals(code)];
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

        final Set<CodeElement> visited = new HashSet<>();
        visited.add(start);

        final List<CodeElement> allElements = code.elementList();

        final Map<Label, CodeElement> labelToCodeElements = new HashMap<>();
        for (final CodeElement elem : allElements) {
            if (elem instanceof final LabelTarget labelTarget) {
                labelToCodeElements.put(labelTarget.label(), elem);
            }
        }

        while (!tasks.isEmpty()) {
            final InterpretTask task = tasks.pop();
            final Status status = task.status;

            System.out.print("Analyzing basic block at #");
            System.out.print(task.eleementIndex);
            System.out.println();

            int elementIndex = task.eleementIndex;
            CodeElement current = allElements.get(elementIndex);
            while (current != null) {
                if (current instanceof final LabelTarget labelnode && !visited.contains(current)) {
                    visited.add(current);
                    final InterpretTask newTask = new InterpretTask();
                    newTask.eleementIndex = allElements.indexOf(labelnode);
                    newTask.status = incomingStatusFor(status, labelnode.label());

                    Node startFlow = status.node;

                    // Create PHI assignments / initializations here...
                    final List<CodeElement> preds = predecessors.get(labelnode.label());
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
                    final LabelNode next = ir.createLabel(labelnode.label());
                    newTask.status.node = startFlow.controlFlowsTo(next, ControlType.FORWARD, task.condition);
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

                visitNode(current, status);

                visited.add(current);

                if (current instanceof final BranchInstruction jumpInsnNode) {
                    if (!visited.contains(labelToCodeElements.get(jumpInsnNode.target()))) {
                        final InterpretTask newTask = new InterpretTask();
                        newTask.eleementIndex = allElements.indexOf(labelToCodeElements.get(jumpInsnNode.target()));
                        newTask.status = incomingStatusFor(status, jumpInsnNode.target());
                        switch (jumpInsnNode.opcode()) {
                            case Opcode.IFEQ:
                            case Opcode.IF_ICMPGE: {
                                newTask.condition = ControlFlowConditionOnTrue.INSTANCE;
                                break;
                            }
                        }
                        tasks.push(newTask);
                    }
                    if (jumpInsnNode.opcode() == Opcode.GOTO) {
                        // Unconditional jump
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

                elementIndex++;
                final CodeElement next = allElements.get(elementIndex);
                if (visited.contains(next)) {
                    break;
                }
                current = next;
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

    private void visitNode(final CodeElement node, final Status status) {

        if (node instanceof final PseudoInstruction psi) {
            // Pseudo Instructions
            switch (psi) {
                case final LabelTarget labelTarget -> visitLabelTarget(labelTarget, status);
                case final LineNumber lineNumber -> visitLineNumberNode(lineNumber, status);
                case final LocalVariable localVariable -> {
                    // Maybe we can use this for debugging?
                }
                default -> throw new IllegalArgumentException("Not implemented yet : " + psi);
            }
        } else if (node instanceof final Instruction ins) {
            // Real bytecode instructions
            switch (ins) {
                case final IncrementInstruction incrementInstruction -> parse_IINC(incrementInstruction, status);
                case final InvokeInstruction invokeInstruction -> visitInvokeInstruction(invokeInstruction, status);
                case final LoadInstruction load -> visitLoadInstruction(load, status);
                case final StoreInstruction store -> visitStoreInstruction(store, status);
                case final BranchInstruction branchInstruction -> visitBranchInstruction(branchInstruction, status);
                case final ConstantInstruction constantInstruction -> visitConstantInstruction(constantInstruction, status);
                case final FieldInstruction fieldInstruction -> visitFieldInstruction(fieldInstruction, status);
                case final NewObjectInstruction newObjectInstruction ->
                        visitNewObjectInstruction(newObjectInstruction, status);
                case final ReturnInstruction returnInstruction -> visitReturnInstruction(returnInstruction, status);
                case final InvokeDynamicInstruction invokeDynamicInstruction ->
                        parse_INVOKEDYNAMIC(invokeDynamicInstruction, status);
                case final TypeCheckInstruction typeCheckInstruction -> parse_CHECKCAST(typeCheckInstruction, status);
                case final StackInstruction stackInstruction -> visitStackInstruction(stackInstruction, status);
                case final OperatorInstruction operatorInstruction -> visitOperatorInstruction(operatorInstruction, status);
                default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void visitOperatorInstruction(final OperatorInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case Opcode.IADD -> parse_IADD(ins, status);
            case Opcode.DADD -> parse_DADD(ins, status);
            default -> throw new IllegalArgumentException("Not implemented yet : " + ins);
        }
    }

    private void visitStackInstruction(final StackInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case Opcode.DUP: {
                parse_DUP(ins, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        }
    }

    private void visitReturnInstruction(final ReturnInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case Opcode.RETURN: {
                parse_RETURN(ins, status);
                break;
            }
            case Opcode.IRETURN: {
                parse_IRETURN(ins, status);
                break;
            }
            case Opcode.DRETURN: {
                parse_DRETURN(ins, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        }
    }

    private void visitNewObjectInstruction(final NewObjectInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case Opcode.NEW: {
                parse_NEW(ins, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        }
    }

    private void visitFieldInstruction(final FieldInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case GETFIELD: {
                parse_GETFIELD(ins, status);
                break;
            }
            case GETSTATIC: {
                parse_GETSTATIC(ins, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        }
    }

    private void visitConstantInstruction(final ConstantInstruction ins, final Status status) {
        switch (ins.opcode()) {
            case Opcode.LDC: {
                parse_LDC(ins, status);
                break;
            }
            case Opcode.ICONST_0: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.ICONST_1: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.ICONST_2: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.ICONST_3: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.ICONST_4: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.ICONST_5: {
                parse_ICONST(ins, status);
                break;
            }
            case Opcode.BIPUSH: {
                parse_BIPUSH(ins, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + ins);
            }
        }
    }

    private void parse_INVOKEDYNAMIC(final InvokeDynamicInstruction node, final Status status) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();
        System.out.println("  opcode INVOKEDYNAMIC Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
        }
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            // TODO: Create invocation here
            status.stack.push(new Result(returnType));
        }
    }

    private void parse_NEW(final NewObjectInstruction node, final Status status) {
        System.out.println("  opcode NEW Creating " + node.className().asSymbol());
        final ClassDesc type = node.className().asSymbol();

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(type);
        final ClassInitialization init = new ClassInitialization(ri);

        status.node = status.node.controlFlowsTo(init, ControlType.FORWARD);

        final Result r = new Result(type);
        final New n = new New(ri);
        final Copy c = new Copy(n, r);

        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_CHECKCAST(final TypeCheckInstruction node, final Status status) {
        System.out.println("  opcode CHECKCAST Checking for " + node.type().asSymbol());
    }

    private void parse_IINC(final IncrementInstruction node, final Status status) {
        // A node that represents an IINC instruction.
        System.out.println("  opcode IINC Local " + node.slot() + " Increment " + node.constant());

        final Result r = new Result(ConstantDescs.CD_int);
        final Copy c = new Copy(new Add(ConstantDescs.CD_int, status.locals[node.slot()], ir.definePrimitiveInt(node.constant())), r);

        status.locals[node.slot()] = r;
        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_IF_ICMPGE(final BranchInstruction node, final Status status) {
        System.out.println("  opcode IFCMP_GE Target " + node.target());
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

    private void parse_IFEQ(final BranchInstruction node, final Status status) {
        System.out.println("  opcode IFEQ Target " + node.target());
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Need a value on stack for comparison");
        }
        final Value v = status.stack.pop();

        final Compare compare = new Compare(Compare.Operation.EQ, v, ir.definePrimitiveInt(0));
        final If next = new If(compare);

        //next.controlFlowsTo(ir.createLabel(node.label), ControlType.FORWARD, ControlFlowConditionOnTrue.INSTANCE);

        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_GOTO(final BranchInstruction node, final Status status) {
        System.out.println("  opcode GOTO Target " + node.target());

        final LabelNode label = ir.createLabel(node.target());

        final Status incoming = incomingStatusFor(status, node.target());

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

    private void visitBranchInstruction(final BranchInstruction node, final Status status) {
        // A node that represents a jump instruction. A jump instruction is an instruction that may jump to another instruction.
        switch (node.opcode()) {
            case Opcode.IF_ICMPGE: {
                parse_IF_ICMPGE(node, status);
                break;
            }
            case Opcode.IFEQ: {
                parse_IFEQ(node, status);
                break;
            }
            case Opcode.GOTO: {
                parse_GOTO(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node);
            }
        }
    }

    private void parse_BIPUSH(final ConstantInstruction node, final Status status) {
        System.out.println("  opcode " + node.opcode());
        status.stack.push(ir.definePrimitiveByte((Integer) node.constantValue()));
    }

    private void parse_LDC(final ConstantInstruction node, final Status status) {
        // A node that represents an LDC instruction.
        System.out.println("  opcode LDC Constant " + node.constantValue());
        if (node.constantValue() instanceof final String str) {
            status.stack.push(new StringConstant(str));
        } else if (node.constantValue() instanceof final ClassDesc classDesc) {
            status.stack.push(ir.defineRuntimeclassReference(classDesc));
        } else {
            throw new IllegalArgumentException("Not implemented yet : " + node);
        }
    }

    private void parse_GETSTATIC(final FieldInstruction node, final Status status) {
        System.out.println("  opcode GETSTATIC Field " + node.field());

        final RuntimeclassReference ri = ir.defineRuntimeclassReference(node.field().owner().asSymbol());
        final ClassInitialization init = new ClassInitialization(ri);

        status.node = status.node.controlFlowsTo(init, ControlType.FORWARD);

        final GetStaticField get = new GetStaticField(node, ri);

        final Result r = new Result(get.type);
        final Copy c = new Copy(get, r);
        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_GETFIELD(final FieldInstruction node, final Status status) {
        System.out.println("  opcode GETFIELD Field " + node.field());
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot load field from empty stack");
        }
        final Value v = status.stack.pop();
        if (v.type.isPrimitive() || v.type.isArray()) {
            throw new IllegalStateException("Cannot load field from non object value " + v);
        }
        final GetInstanceField get = new GetInstanceField(node, v);
        final Result r = new Result(get.type);
        final Copy c = new Copy(get, r);
        status.stack.push(r);

        status.node = status.node.controlFlowsTo(c, ControlType.FORWARD);
    }

    private void parse_RETURN(final ReturnInstruction node, final Status status) {
        System.out.println("  opcode RETURN");

        final Return next = new Return();
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_ICONST(final ConstantInstruction node, final Status status) {
        System.out.println("  opcode ICONST " + node.constantValue());
        status.stack.push(ir.definePrimitiveInt((Integer) node.constantValue()));
    }

    private void parse_IADD(final OperatorInstruction node, final Status status) {
        System.out.println("  opcode IADD");
        if (status.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for addition");
        }
        final Value a = status.stack.pop();
        if (!a.type.equals(ConstantDescs.CD_int)) {
            throw new IllegalStateException("Cannot add non int value " + a + " for addition");
        }
        final Value b = status.stack.pop();
        if (!b.type.equals(ConstantDescs.CD_int)) {
            throw new IllegalStateException("Cannot add non int value " + b + " for addition");
        }
        final Add add = new Add(ConstantDescs.CD_int, b, a);
        status.stack.push(add);
    }

    private void parse_DADD(final OperatorInstruction node, final Status status) {
        System.out.println("  opcode DADD");
        if (status.stack.size() < 2) {
            throw new IllegalStateException("Need a minium of two values on stack for addition");
        }
        final Value a = status.stack.pop();
        if (!a.type.equals(ConstantDescs.CD_double)) {
            throw new IllegalStateException("Cannot add non double value " + a + " for addition");
        }
        final Value b = status.stack.pop();
        if (!b.type.equals(ConstantDescs.CD_double)) {
            throw new IllegalStateException("Cannot add non double value " + b + " for addition");
        }
        final Add add = new Add(ConstantDescs.CD_double, b, a);
        status.stack.push(add);
    }

    private void parse_IRETURN(final ReturnInstruction node, final Status status) {
        System.out.println("  opcode IRETURN");
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot return empty stack");
        }
        final Value v = status.stack.pop();
        if (!v.type.equals(ConstantDescs.CD_int)) {
            throw new IllegalStateException("Cannot return non int value " + v);
        }
        final ReturnValue next = new ReturnValue(ConstantDescs.CD_int, v);
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_DRETURN(final ReturnInstruction node, final Status status) {
        System.out.println("  opcode DRETURN");
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot return empty stack");
        }
        final Value v = status.stack.pop();
        if (!v.type.equals(ConstantDescs.CD_double)) {
            throw new IllegalStateException("Cannot return non double value " + v);
        }
        final ReturnValue next = new ReturnValue(ConstantDescs.CD_double, v);
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);
    }

    private void parse_DUP(final StackInstruction node, final Status status) {
        System.out.println("  " + node + " opcode DUP");
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot duplicate empty stack");
        }
        final Value v = status.stack.peek();
        status.stack.push(v);
    }

    private void parse_ALOAD(final LoadInstruction node, final Status status) {
        System.out.println("  opcode ALOAD Local " + node.slot());
        final Value v = status.locals[node.slot()];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.slot());
        }
        status.stack.push(v);
    }

    private void parse_ILOAD(final LoadInstruction node, final Status status) {
        System.out.println("  opcode ILOAD Local " + node.slot());
        final Value v = status.locals[node.slot()];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.slot());
        }
        if (!v.type.equals(ConstantDescs.CD_int)) {
            throw new IllegalStateException("Cannot load non int value " + v + " for index " + node.slot());
        }
        status.stack.push(v);
    }

    private void parse_DLOAD(final LoadInstruction node, final Status status) {
        System.out.println("  opcode DLOAD Local " + node.slot());
        final Value v = status.locals[node.slot()];
        if (v == null) {
            throw new IllegalStateException("Cannot local is null for index " + node.slot());
        }
        if (!v.type.equals(ConstantDescs.CD_double)) {
            throw new IllegalStateException("Cannot load non double value " + v + " for index " + node.slot());
        }
        status.stack.push(v);
    }

    private void parse_ISTORE(final StoreInstruction node, final Status status) {
        System.out.println("  opcode ISTORE Local " + node.slot());
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
        }
        status.locals[node.slot()] = status.stack.pop();
        if (node.slot() > 0) {
            if (status.locals[node.slot() - 1] != null && needsTwoSlots(status.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                status.locals[node.slot() - 1] = null;
            }
        }
    }

    private void parse_ASTORE(final StoreInstruction node, final Status status) {
        System.out.println("  opcode ASTORE Local " + node.slot());
        if (status.stack.isEmpty()) {
            throw new IllegalStateException("Cannot store empty stack");
        }
        status.locals[node.slot()] = status.stack.pop();
        if (node.slot() > 0) {
            if (status.locals[node.slot() - 1] != null && needsTwoSlots(status.locals[node.slot() - 1].type)) {
                // Remove potential illegal values
                status.locals[node.slot() - 1] = null;
            }
        }
    }

    private void visitLoadInstruction(final LoadInstruction node, final Status status) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (node.opcode()) {
            case Opcode.ALOAD: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcode.ALOAD_0: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcode.ALOAD_1: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcode.ALOAD_2: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcode.ALOAD_3: {
                parse_ALOAD(node, status);
                break;
            }
            case Opcode.ILOAD: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcode.ILOAD_0: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcode.ILOAD_1: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcode.ILOAD_2: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcode.ILOAD_3: {
                parse_ILOAD(node, status);
                break;
            }
            case Opcode.DLOAD: {
                parse_DLOAD(node, status);
                break;
            }
            case Opcode.DLOAD_0: {
                parse_DLOAD(node, status);
                break;
            }
            case Opcode.DLOAD_1: {
                parse_DLOAD(node, status);
                break;
            }
            case Opcode.DLOAD_2: {
                parse_DLOAD(node, status);
                break;
            }
            case Opcode.DLOAD_3: {
                parse_DLOAD(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node);
            }
        }
    }

    private void visitStoreInstruction(final StoreInstruction node, final Status status) {
        // A node that represents a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
        switch (node.opcode()) {
            case Opcode.ASTORE: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcode.ASTORE_0: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcode.ASTORE_1: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcode.ASTORE_2: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcode.ASTORE_3: {
                parse_ASTORE(node, status);
                break;
            }
            case Opcode.ISTORE: {
                parse_ISTORE(node, status);
                break;
            }
            case Opcode.ISTORE_0: {
                parse_ISTORE(node, status);
                break;
            }
            case Opcode.ISTORE_1: {
                parse_ISTORE(node, status);
                break;
            }
            case Opcode.ISTORE_2: {
                parse_ISTORE(node, status);
                break;
            }
            case Opcode.ISTORE_3: {
                parse_ISTORE(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node);
            }
        }
    }

    private void parse_INVOKESPECIAL(final InvokeInstruction node, final Status status) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKESPECIAL Method " + node.name() + " desc " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            arguments.add(status.stack.pop());
        }

        final Value next = new Invocation(node, arguments.reversed());
        status.node = status.node.controlFlowsTo(next, ControlType.FORWARD);

        if (!returnType.equals(ConstantDescs.CD_void)) {
            status.stack.push(next);
        }
    }

    private void parse_INVOKEVIRTUAL(final InvokeInstruction node, final Status status) {

        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKEVIRTUAL Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void parse_INVOKEINTERFACE(final InvokeInstruction node, final Status status) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKEINTERFACE Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = 1 + args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void parse_INVOKESTATIC(final InvokeInstruction node, final Status status) {
        final MethodTypeDesc methodTypeDesc = node.typeSymbol();

        System.out.println("  opcode INVOKESTATIC Method " + node.name() + " " + methodTypeDesc);
        final ClassDesc returnType = methodTypeDesc.returnType();
        final ClassDesc[] args = methodTypeDesc.parameterArray();
        final int expectedarguments = args.length;
        if (status.stack.size() < expectedarguments) {
            throw new IllegalStateException("Not enough arguments on stack for " + node.name() + " " + methodTypeDesc + " expected " + expectedarguments + " but got " + status.stack.size());
        }
        final List<Value> arguments = new ArrayList<>();
        for (int i = 0; i < expectedarguments; i++) {
            final Value v = status.stack.pop();
            arguments.add(v);
        }
        final Invocation invocation = new Invocation(node, arguments.reversed());
        if (!returnType.equals(ConstantDescs.CD_void)) {
            final Result r = new Result(returnType);
            final Copy copy = new Copy(invocation, r);
            status.stack.push(r);
            status.node = status.node.controlFlowsTo(copy, ControlType.FORWARD);
        } else {
            status.node = status.node.controlFlowsTo(invocation, ControlType.FORWARD);
        }
    }

    private void visitInvokeInstruction(final InvokeInstruction node, final Status status) {
        // A node that represents a method instruction. A method instruction is an instruction that invokes a method.
        switch (node.opcode()) {
            case Opcode.INVOKESPECIAL: {
                parse_INVOKESPECIAL(node, status);
                break;
            }
            case Opcode.INVOKEVIRTUAL: {
                parse_INVOKEVIRTUAL(node, status);
                break;
            }
            case Opcode.INVOKEINTERFACE: {
                parse_INVOKEINTERFACE(node, status);
                break;
            }
            case Opcode.INVOKESTATIC: {
                parse_INVOKESTATIC(node, status);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented yet : " + node);
            }
        }
    }

    private void visitLabelTarget(final LabelTarget node, final Status status) {
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
    }

    private void visitLineNumberNode(final LineNumber node, final Status status) {
        // A node that represents a line number declaration. These nodes are pseudo instruction nodes in order to be inserted in an instruction list.
        status.lineNumber = node.line();
        System.out.println("  Line " + node.line());
    }
}
