package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StructuredControlflowCodeGenerator<T extends StructuredControlflowCodeGenerator.GeneratedThing> {

    public interface GeneratedThing {

    }
    private final Map<Node, T> committedToTemporary;

    public StructuredControlflowCodeGenerator() {
        committedToTemporary = new HashMap<>();
    }

    public abstract void begin(final Method method);

    public abstract void startBlock(final Sequencer.Block b);

    public abstract void writeBreakTo(final String label, final Node currentNode, final Node targetNode);

    public abstract void writeContinueTo(final String label, final Node currentNode, final Node targetNode);

    public abstract void finishBlock(final Sequencer.Block block);

    public abstract void writePHINodesFor(final Node node);

    public abstract void write(final Return node);

    public abstract void write(final ReturnValue node);

    public abstract void writePreGoto(final Goto node);

    public abstract void write(final Throw node);

    public abstract void write(final ArrayStore node);

    public abstract void write(final PutStatic node);

    public abstract void write(final PutField node);

    public abstract void write(final MonitorEnter node);

    public abstract void write(final MonitorExit node);

    public abstract void write(final ClassInitialization node);

    public abstract void write(final Div node);

    public abstract void write(final Rem node);

    public abstract void write(final ArrayLoad node);

    public abstract void write(final InvokeSpecial node);

    public abstract void write(final InvokeInterface node);

    public abstract void write(final InvokeVirtual node);

    public abstract void write(final InvokeStatic node);

    public abstract void write(final InvokeDynamic node);

    public abstract void write(final LabelNode node);

    public abstract void write(final MergeNode node);

    public abstract void write(final CheckCast node);

    public abstract void startIfWithTrueBlock(final If node);

    public abstract void startIfElseBlock(final If node);

    public abstract void finishIfBlock();

    public abstract void startLookupSwitch(final LookupSwitch node);

    public abstract void writeSwitchCase(final int key);

    public abstract void finishSwitchCase();

    public abstract void writeSwitchDefaultCase();

    public abstract void finishSwitchDefault();

    public abstract void finishLookupSwitch();

    public abstract void startTableSwitch(final TableSwitch node);

    public abstract void startTableSwitchDefaultBlock();

    public abstract void finishTableSwitchDefaultBlock();

    public abstract void finishTableSwitch();

    public abstract void startTryCatch(final ExceptionGuard node);

    public abstract void startCatchBlock();

    public abstract void startCatchHandler(final List<ClassDesc> exceptionTypes);

    public abstract void writeRethrowException();

    public abstract void finishCatchHandler();

    public abstract void finishTryCatch();

    public abstract void finished();

    // Expression / Value generation
    public abstract T visit_PrimitiveInt(final PrimitiveInt node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_PrimitiveLong(PrimitiveLong node, Deque<Node> expressionStack, Deque<T> evaluationStack);

    public abstract T visit_PrimitiveFloat(PrimitiveFloat node, Deque<Node> expressionStack, Deque<T> evaluationStack);

    public abstract T visit_PrimitiveDouble(PrimitiveDouble node, Deque<Node> expressionStack, Deque<T> evaluationStack);

    public abstract T visit_Add(final Add node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Div(final Div node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Mul(final Mul node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Sub(final Sub node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Rem(final Rem node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ExtractThisRefProjection(ExtractThisRefProjection node, Deque<Node> expressionStack, Deque<T> evaluationStack);

    public abstract T visit_ExtractMethodArgProjection(final ExtractMethodArgProjection node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_StringConstant(final StringConstant node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Null(final Null node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Negate(final Negate node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ArrayLength(final ArrayLength node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_NewArray(final NewArray node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ArrayLoad(final ArrayLoad node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_BitOperation(final BitOperation node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_GetField(final GetField node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_GetStatic(final GetStatic node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_RuntimeClassReference(final RuntimeclassReference node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ReferenceCondition(final ReferenceCondition node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ReferenceTest(final ReferenceTest node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_New(final New node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_NumericCondition(final NumericCondition node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_VarArgsArray(final VarArgsArray node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Convert(final Convert node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InstanceOf(final InstanceOf node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Truncate(final Truncate node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_Extend(final Extend node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_ClassInitialization(final ClassInitialization node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InvokeInterface(final InvokeInterface node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InvokeSpecial(final InvokeSpecial node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InvokeVirtual(final InvokeVirtual node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InvokeStatic(final InvokeStatic node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_NewMultiArray(final NewMultiArray node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_InvokeDynamic(final InvokeDynamic node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_PHI(final PHI node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_MethodType(final MethodType node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_MethodHandle(final MethodHandle node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract T visit_CaughtExceptionProjection(final CaughtExceptionProjection node, final Deque<Node> expressionStack, final Deque<T> evaluationStack);

    public abstract void emitFinally(final T result);

    private T visit(final Node node, final Deque<Node> expressionStack, final Deque<T> evaluationStack) {
        final T alreadyCreated = committedToTemporary.get(node);
        if (alreadyCreated != null) {
            return alreadyCreated;
        }

        if (node instanceof final Add add) {
            return visit_Add(add, expressionStack, evaluationStack);
        } else if (node instanceof final Div div) {
            return visit_Div(div, expressionStack, evaluationStack);
        } else if (node instanceof final Mul mul) {
            return visit_Mul(mul, expressionStack, evaluationStack);
        } else if (node instanceof final Sub sub) {
            return visit_Sub(sub, expressionStack, evaluationStack);
        } else if (node instanceof final Rem rem) {
            return visit_Rem(rem, expressionStack, evaluationStack);
        } else if (node instanceof final PrimitiveInt primitiveInt) {
            return visit_PrimitiveInt(primitiveInt, expressionStack, evaluationStack);
        } else if (node instanceof final PrimitiveLong primitiveLong) {
            return visit_PrimitiveLong(primitiveLong, expressionStack, evaluationStack);
        } else if (node instanceof final PrimitiveFloat primitiveFloat) {
            return visit_PrimitiveFloat(primitiveFloat, expressionStack, evaluationStack);
        } else if (node instanceof final PrimitiveDouble primitiveDouble) {
            return visit_PrimitiveDouble(primitiveDouble, expressionStack, evaluationStack);
        } else if (node instanceof final ExtractMethodArgProjection extractMethodArgProjection) {
            return visit_ExtractMethodArgProjection(extractMethodArgProjection, expressionStack, evaluationStack);
        } else if (node instanceof final ExtractThisRefProjection extractThisRefProjection) {
            return visit_ExtractThisRefProjection(extractThisRefProjection, expressionStack, evaluationStack);
        } else if (node instanceof final StringConstant stringConstant) {
            return visit_StringConstant(stringConstant, expressionStack, evaluationStack);
        } else if (node instanceof final Null nu) {
            return visit_Null(nu, expressionStack, evaluationStack);
        } else if (node instanceof final Negate negate) {
            return visit_Negate(negate, expressionStack, evaluationStack);
        } else if (node instanceof final ArrayLength length) {
            return visit_ArrayLength(length, expressionStack, evaluationStack);
        } else if (node instanceof final NewArray newArray) {
            return visit_NewArray(newArray, expressionStack, evaluationStack);
        } else if (node instanceof final ArrayLoad arrayLoad) {
            return visit_ArrayLoad(arrayLoad, expressionStack, evaluationStack);
        } else if (node instanceof final BitOperation bitOperation) {
            return visit_BitOperation(bitOperation, expressionStack, evaluationStack);
        } else if (node instanceof final GetField getField) {
            return visit_GetField(getField, expressionStack, evaluationStack);
        } else if (node instanceof final GetStatic getStatic) {
            return visit_GetStatic(getStatic, expressionStack, evaluationStack);
        } else if (node instanceof final RuntimeclassReference classReference) {
            return visit_RuntimeClassReference(classReference, expressionStack, evaluationStack);
        } else if (node instanceof final ReferenceCondition referenceCondition) {
            return visit_ReferenceCondition(referenceCondition, expressionStack, evaluationStack);
        } else if (node instanceof final ReferenceTest referenceTest) {
            return visit_ReferenceTest(referenceTest, expressionStack, evaluationStack);
        } else if (node instanceof final New n) {
            return visit_New(n, expressionStack, evaluationStack);
        } else if (node instanceof final NumericCondition n) {
            return visit_NumericCondition(n, expressionStack, evaluationStack);
        } else if (node instanceof final VarArgsArray varArgsArray) {
            return visit_VarArgsArray(varArgsArray, expressionStack, evaluationStack);
        } else if (node instanceof final Convert convert) {
            return visit_Convert(convert, expressionStack, evaluationStack);
        } else if (node instanceof final InstanceOf io) {
            return visit_InstanceOf(io, expressionStack, evaluationStack);
        } else if (node instanceof final Truncate truncate) {
            return visit_Truncate(truncate, expressionStack, evaluationStack);
        } else if (node instanceof final Extend extend) {
            return visit_Extend(extend, expressionStack, evaluationStack);
        } else if (node instanceof final ClassInitialization init) {
            return visit_ClassInitialization(init, expressionStack, evaluationStack);
        } else if (node instanceof final InvokeInterface invoke) {
            return visit_InvokeInterface(invoke, expressionStack, evaluationStack);
        } else if (node instanceof final InvokeSpecial invoke) {
            return visit_InvokeSpecial(invoke, expressionStack, evaluationStack);
        } else if (node instanceof final InvokeVirtual invoke) {
            return visit_InvokeVirtual(invoke, expressionStack, evaluationStack);
        } else if (node instanceof final InvokeStatic invoke) {
            return visit_InvokeStatic(invoke, expressionStack, evaluationStack);
        } else if (node instanceof final NewMultiArray newMultiArray) {
            return visit_NewMultiArray(newMultiArray, expressionStack, evaluationStack);
        } else if (node instanceof final InvokeDynamic invokeDynamic) {
            return visit_InvokeDynamic(invokeDynamic, expressionStack, evaluationStack);
        } else if (node instanceof final PHI phi) {
            return visit_PHI(phi, expressionStack, evaluationStack);
        } else if (node instanceof final MethodType methodType) {
            return visit_MethodType(methodType, expressionStack, evaluationStack);
        } else if (node instanceof final MethodHandle methodHandle) {
            return visit_MethodHandle(methodHandle, expressionStack, evaluationStack);
        } else if (node instanceof final CaughtExceptionProjection caughtExceptionProjection) {
            return visit_CaughtExceptionProjection(caughtExceptionProjection, expressionStack, evaluationStack);
        }

        throw new IllegalArgumentException("Unsupported node " + node+ " of type " + node.getClass());
    }

    public abstract T emitTemporaryVariable(final String prefix, final T value);

    protected final void emitWithTemporary(final String prefix, final Node node, final T value, final Deque<T> evaluationStack) {
        final T committed = committedToTemporary.get(node);
        if (committed == null) {
            final T temporary = emitTemporaryVariable(prefix, value);
            committedToTemporary.put(node, temporary);
            evaluationStack.push(temporary);
        } else {
            evaluationStack.push(committed);
        }
    }

    protected final void emitWithTemporary(final Node node, final T value, final Deque<T> evaluationStack) {
        emitWithTemporary("var", node, value, evaluationStack);
    }

    protected T commitedTemporaryFor(final Node node) {
        return committedToTemporary.get(node);
    }

    protected final void emit(final Node node, final Deque<Node> expressionStack, final Deque<T> evaluationStack) {
        try {
            expressionStack.push(node);

            final T result = visit(node, expressionStack, evaluationStack);

            if (!node.isConstant() && node.isDataUsedMultipleTimes()) {
                emitWithTemporary(node, result, evaluationStack);
            } else {
                evaluationStack.push(result);
            }
        } finally {
            expressionStack.pop();
        }
    }

    public final void emit(final Node node) {
        final Deque<T> stack = new ArrayDeque<>();
        emit(node, new ArrayDeque<>(), stack);
        while (!stack.isEmpty()) {
            emitFinally(stack.pop());
        }
    }
}
