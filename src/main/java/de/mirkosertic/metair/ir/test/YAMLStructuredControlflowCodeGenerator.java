package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.Add;
import de.mirkosertic.metair.ir.Div;
import de.mirkosertic.metair.ir.ExceptionGuard;
import de.mirkosertic.metair.ir.ExtractMethodArgProjection;
import de.mirkosertic.metair.ir.ExtractThisRefProjection;
import de.mirkosertic.metair.ir.Goto;
import de.mirkosertic.metair.ir.If;
import de.mirkosertic.metair.ir.LookupSwitch;
import de.mirkosertic.metair.ir.Method;
import de.mirkosertic.metair.ir.Mul;
import de.mirkosertic.metair.ir.Node;
import de.mirkosertic.metair.ir.PrimitiveDouble;
import de.mirkosertic.metair.ir.PrimitiveFloat;
import de.mirkosertic.metair.ir.PrimitiveInt;
import de.mirkosertic.metair.ir.PrimitiveLong;
import de.mirkosertic.metair.ir.Rem;
import de.mirkosertic.metair.ir.Return;
import de.mirkosertic.metair.ir.ReturnValue;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.ir.StringConstant;
import de.mirkosertic.metair.ir.StructuredControlflowCodeGenerator;
import de.mirkosertic.metair.ir.Sub;
import de.mirkosertic.metair.ir.TableSwitch;
import de.mirkosertic.metair.ir.Throw;
import de.mirkosertic.metair.ir.TypeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.util.Deque;
import java.util.List;

public class YAMLStructuredControlflowCodeGenerator extends StructuredControlflowCodeGenerator<YAMLStructuredControlflowCodeGenerator.GeneratedCode> {

    public record GeneratedCode(ConstantDesc type, String value) implements GeneratedThing {

        @Override
        public String toString() {
            return value;
        }
    }

    private final StringWriter sw;
    private final PrintWriter pw;
    private int temporaryVariablesCounter;

    public YAMLStructuredControlflowCodeGenerator() {
        this.sw = new StringWriter();
        this.pw = new PrintWriter(sw);
        this.temporaryVariablesCounter = 0;
    }

    // Expression generation

    @Override
    public GeneratedCode visit_PrimitiveInt(final PrimitiveInt node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(ConstantDescs.CD_int, Integer.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveLong(final PrimitiveLong node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(ConstantDescs.CD_long, Long.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveFloat(final PrimitiveFloat node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(ConstantDescs.CD_float, Float.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveDouble(final PrimitiveDouble node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(ConstantDescs.CD_double, Double.toString(node.value));
    }

    @Override
    public GeneratedCode visit_Add(final Add node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        emit(node.arg1, expressionStack, evaluationStack);
        emit(node.arg2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " + " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Div(final Div node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        emit(node.arg1, expressionStack, evaluationStack);
        emit(node.arg2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " / " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Mul(final Mul node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        emit(node.arg1, expressionStack, evaluationStack);
        emit(node.arg2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " * " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Sub(final Sub node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        emit(node.arg1, expressionStack, evaluationStack);
        emit(node.arg2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " - " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Rem(final Rem node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        emit(node.arg1, expressionStack, evaluationStack);
        emit(node.arg2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " % " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_ExtractThisRefProjection(final ExtractThisRefProjection node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(node.type, node.name());
    }

    @Override
    public GeneratedCode visit_ExtractMethodArgProjection(final ExtractMethodArgProjection node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(node.type, node.name());
    }

    @Override
    public GeneratedCode visit_StringConstant(final StringConstant node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(ConstantDescs.CD_String, "\"" + node.value + "\"");
    }

    @Override
    public GeneratedCode emitTemporaryVariable(final GeneratedCode value) {
        final String varname = "var" + temporaryVariablesCounter++;
        pw.print(TypeUtils.toString(value.type));
        pw.print(" ");
        pw.print(varname);
        pw.print(" = ");
        pw.print(value);
        pw.print(System.lineSeparator());

        return new GeneratedCode(value.type, varname);
    }

    @Override
    public void emitFinally(final GeneratedCode result) {
        pw.println(result.value);
    }

    @Override
    public String toString() {
        pw.flush();
        sw.flush();
        return sw.toString().trim();
    }

    // CFG - Generation


    @Override
    public void begin(final Method method) {
    }

    @Override
    public void writeBreakTo(final String label) {
    }

    @Override
    public void writeContinueTo(final String label) {
    }

    @Override
    public void finishBlock(final Sequencer.Block block, final boolean lastBlock) {
    }

    @Override
    public void startBlock(final Sequencer.Block b) {
    }

    @Override
    public void startIfWithTrueBlock(final If node) {
    }

    @Override
    public void startIfElseBlock(final If node) {
    }

    @Override
    public void finishIfBlock() {
    }

    @Override
    public void write(final Return node) {
    }

    @Override
    public void write(final ReturnValue node) {
    }

    @Override
    public void write(final Goto node) {
    }

    @Override
    public void write(final Throw node) {
    }

    @Override
    public void startLookupSwitch(final LookupSwitch node) {
    }

    @Override
    public void writeSwitchCase(final int key) {
    }

    @Override
    public void finishSwitchCase() {
    }

    @Override
    public void writeSwitchDefaultCase() {
    }

    @Override
    public void finishSwitchDefault() {
    }

    @Override
    public void finishLookupSwitch() {
    }

    @Override
    public void startTableSwitch(final TableSwitch node) {
    }

    @Override
    public void startTableSwitchDefaultBlock() {
    }

    @Override
    public void finishTableSwitchDefaultBlock() {
    }

    @Override
    public void finishTableSwitch() {
    }

    @Override
    public void startTryCatch(final ExceptionGuard node) {
    }

    @Override
    public void startCatchBlock() {
    }

    @Override
    public void startCatchHandler(final List<ClassDesc> exceptionTypes) {
    }

    @Override
    public void writeRethrowException() {
    }

    @Override
    public void finishCatchHandler() {
    }

    @Override
    public void finishTryCatch() {
    }
}
