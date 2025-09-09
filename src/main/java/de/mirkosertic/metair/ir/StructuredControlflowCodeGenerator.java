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

    public abstract void writeBreakTo(final String label);

    public abstract void writeContinueTo(final String label);

    public abstract void finishBlock(final Sequencer.Block block, boolean lastBlock);

    public abstract void write(final Return node);

    public abstract void write(final ReturnValue node);

    public abstract void write(final Goto node);

    public abstract void write(final Throw node);

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
        }
        throw new IllegalArgumentException("Not yet implemented : " + node + " of type " + node.getClass());
    }

    public abstract T emitTemporaryVariable(final T value);

    protected void emit(final Node node, final Deque<Node> expressionStack, final Deque<T> evaluationStack) {
        try {
            expressionStack.push(node);

            final T result = visit(node, expressionStack, evaluationStack);

            if (!node.isConstant() && node.isDataUsedMultipleTimes()) {
                final T committed = committedToTemporary.get(node);
                if (committed == null) {
                    final T temporary = emitTemporaryVariable(result);
                    committedToTemporary.put(node, temporary);
                    evaluationStack.push(temporary);
                } else {
                    evaluationStack.push(committed);
                }
            } else {
                evaluationStack.push(result);
            }
        } finally {
            expressionStack.pop();
        }
    }

    public void emit(final Node node) {
        final Deque<T> stack = new java.util.ArrayDeque<>();
        emit(node, new ArrayDeque<>(), stack);
        while (!stack.isEmpty()) {
            emitFinally(stack.pop());
        }
    }
}
