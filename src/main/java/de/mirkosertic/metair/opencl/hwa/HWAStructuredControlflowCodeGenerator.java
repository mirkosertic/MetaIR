package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.ir.Add;
import de.mirkosertic.metair.ir.ArrayLength;
import de.mirkosertic.metair.ir.ArrayLoad;
import de.mirkosertic.metair.ir.ArrayStore;
import de.mirkosertic.metair.ir.BitOperation;
import de.mirkosertic.metair.ir.CaughtExceptionProjection;
import de.mirkosertic.metair.ir.CheckCast;
import de.mirkosertic.metair.ir.ClassInitialization;
import de.mirkosertic.metair.ir.Convert;
import de.mirkosertic.metair.ir.Div;
import de.mirkosertic.metair.ir.ExceptionGuard;
import de.mirkosertic.metair.ir.Extend;
import de.mirkosertic.metair.ir.ExtractMethodArgProjection;
import de.mirkosertic.metair.ir.ExtractThisRefProjection;
import de.mirkosertic.metair.ir.GetField;
import de.mirkosertic.metair.ir.GetStatic;
import de.mirkosertic.metair.ir.IRType;
import de.mirkosertic.metair.ir.If;
import de.mirkosertic.metair.ir.InstanceOf;
import de.mirkosertic.metair.ir.InvokeDynamic;
import de.mirkosertic.metair.ir.InvokeInterface;
import de.mirkosertic.metair.ir.InvokeSpecial;
import de.mirkosertic.metair.ir.InvokeStatic;
import de.mirkosertic.metair.ir.InvokeVirtual;
import de.mirkosertic.metair.ir.LabelNode;
import de.mirkosertic.metair.ir.LookupSwitch;
import de.mirkosertic.metair.ir.MergeNode;
import de.mirkosertic.metair.ir.Method;
import de.mirkosertic.metair.ir.MethodHandle;
import de.mirkosertic.metair.ir.MethodType;
import de.mirkosertic.metair.ir.MonitorEnter;
import de.mirkosertic.metair.ir.MonitorExit;
import de.mirkosertic.metair.ir.Mul;
import de.mirkosertic.metair.ir.Negate;
import de.mirkosertic.metair.ir.New;
import de.mirkosertic.metair.ir.NewArray;
import de.mirkosertic.metair.ir.NewMultiArray;
import de.mirkosertic.metair.ir.Node;
import de.mirkosertic.metair.ir.Null;
import de.mirkosertic.metair.ir.NumericCompare;
import de.mirkosertic.metair.ir.NumericCondition;
import de.mirkosertic.metair.ir.PHI;
import de.mirkosertic.metair.ir.PrimitiveDouble;
import de.mirkosertic.metair.ir.PrimitiveFloat;
import de.mirkosertic.metair.ir.PrimitiveInt;
import de.mirkosertic.metair.ir.PrimitiveLong;
import de.mirkosertic.metair.ir.Projection;
import de.mirkosertic.metair.ir.PutField;
import de.mirkosertic.metair.ir.PutStatic;
import de.mirkosertic.metair.ir.ReferenceCondition;
import de.mirkosertic.metair.ir.ReferenceTest;
import de.mirkosertic.metair.ir.Rem;
import de.mirkosertic.metair.ir.Return;
import de.mirkosertic.metair.ir.ReturnValue;
import de.mirkosertic.metair.ir.RuntimeclassReference;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.ir.StringConstant;
import de.mirkosertic.metair.ir.StructuredControlflowCodeGenerator;
import de.mirkosertic.metair.ir.Sub;
import de.mirkosertic.metair.ir.TableSwitch;
import de.mirkosertic.metair.ir.Throw;
import de.mirkosertic.metair.ir.Truncate;
import de.mirkosertic.metair.ir.TypeUtils;
import de.mirkosertic.metair.ir.Value;
import de.mirkosertic.metair.ir.VarArgsArray;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HWAStructuredControlflowCodeGenerator extends StructuredControlflowCodeGenerator<HWAStructuredControlflowCodeGenerator.GeneratedCode> {

    public record GeneratedCode(IRType type, String value) implements StructuredControlflowCodeGenerator.GeneratedThing {

        @Override
        public String toString() {
            return value;
        }
    }

    private final List<HWAKernelArgument> kernelArguments;
    private final StringWriter sw;
    private final PrintWriter pw;
    private int temporaryVariablesCounter;
    private int indentationLevel = 0;
    private final Set<PHI> phiNodes;

    public HWAStructuredControlflowCodeGenerator(final List<HWAKernelArgument> kernelArguments) {
        this.kernelArguments = kernelArguments;
        this.sw = new StringWriter();
        this.pw = new PrintWriter(sw);
        this.temporaryVariablesCounter = 0;
        this.phiNodes = new HashSet<>();
    }

    private void writeIndentation() {
        for (int i = 0; i < indentationLevel; i++) {
            pw.print("  ");
        }
    }

        // Expression generation

    @Override
    public GeneratedCode visit_PrimitiveInt(final PrimitiveInt node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(IRType.CD_int, Integer.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveLong(final PrimitiveLong node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(IRType.CD_long, Long.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveFloat(final PrimitiveFloat node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(IRType.CD_float, Float.toString(node.value));
    }

    @Override
    public GeneratedCode visit_PrimitiveDouble(final PrimitiveDouble node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(IRType.CD_double, Double.toString(node.value));
    }

    @Override
    public GeneratedCode visit_Add(final Add node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " + " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Div(final Div node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " / " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Mul(final Mul node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " * " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Sub(final Sub node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " - " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Rem(final Rem node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

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
        return new GeneratedCode(IRType.CD_String, "\"" + node.value + "\"");
    }

    @Override
    public GeneratedCode visit_Null(final Null node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(IRType.CD_Object, "null");
    }

    @Override
    public GeneratedCode visit_Negate(final Negate node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();
        return new GeneratedCode(node.type, "(-" + arg1 + ")");
    }

    @Override
    public GeneratedCode visit_ArrayLength(final ArrayLength node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, arg1 + ".length");
    }

    @Override
    public GeneratedCode visit_NewArray(final NewArray node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(new " + TypeUtils.toString(((IRType.MetaClass) node.type).componentType()) + "[" + arg1 + "])");
    }

    @Override
    public GeneratedCode visit_ArrayLoad(final ArrayLoad node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + "[" + arg2 + "])");
    }

    @Override
    public GeneratedCode visit_BitOperation(final BitOperation node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return switch (node.operation) {
            case AND -> new GeneratedCode(node.type, "(" + arg1 + " & " + arg2 + ")");
            case OR -> new GeneratedCode(node.type, "(" + arg1 + " | " + arg2 + ")");
            case XOR -> new GeneratedCode(node.type, "(" + arg1 + " ^ " + arg2 + ")");
            case SHL -> new GeneratedCode(node.type, "(" + arg1 + " << " + arg2 + ")");
            case SHR -> new GeneratedCode(node.type, "(" + arg1 + " >>" + arg2 + ")");
            case USHR -> new GeneratedCode(node.type, "(" + arg1 + " >>>" + arg2 + ")");
        };
    }

    @Override
    public GeneratedCode visit_GetField(final GetField node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + "." + node.fieldName + ")");
    }

    @Override
    public GeneratedCode visit_GetStatic(final GetStatic node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + "." + node.fieldName + ")");
    }

    @Override
    public GeneratedCode visit_RuntimeClassReference(final RuntimeclassReference node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        return new GeneratedCode(node.type, TypeUtils.toString(node.type) + ".class");
    }

    @Override
    public GeneratedCode visit_ReferenceCondition(final ReferenceCondition node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return switch (node.operation) {
            case EQ -> new GeneratedCode(node.type, "(" + arg1 + " == " + arg2 + ")");
            case NE -> new GeneratedCode(node.type, "(" + arg1 + " != " + arg2 + ")");
        };
    }

    @Override
    public GeneratedCode visit_ReferenceTest(final ReferenceTest node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return switch (node.operation) {
            case NULL -> new GeneratedCode(node.type, "(" + arg1 + " == null)");
            case NONNULL -> new GeneratedCode(node.type, "(" + arg1 + " != null)");
        };
    }

    @Override
    public GeneratedCode visit_New(final New node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(new " + arg1 + ")");
    }

    @Override
    public GeneratedCode visit_NumericCondition(final NumericCondition node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return switch (node.operation) {
            case EQ -> new GeneratedCode(node.type, "(" + arg1 + " == " + arg2 + ")");
            case NE -> new GeneratedCode(node.type, "(" + arg1 + " != " + arg2 + ")");
            case GT -> new GeneratedCode(node.type, "(" + arg1 + " > " + arg2 + ")");
            case GE -> new GeneratedCode(node.type, "(" + arg1 + " >= " + arg2 + ")");
            case LT -> new GeneratedCode(node.type, "(" + arg1 + " < " + arg2 + ")");
            case LE -> new GeneratedCode(node.type, "(" + arg1 + " <= " + arg2 + ")");
        };
    }

    @Override
    public GeneratedCode visit_NumericCompare(final NumericCompare node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "numcomp(" + node.compareType + "," + arg1 + ", " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_VarArgsArray(final VarArgsArray node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        final List<Node> argumwens = node.arguments();
        for (final Node arg : argumwens) {
            emit(arg, expressionStack, evaluationStack);
        }

        List<GeneratedCode> content = new ArrayList<>();
        for (int i = 0; i < argumwens.size(); i++) {
            content.add(evaluationStack.pop());
        }

        content = content.reversed();

        final StringBuilder result = new StringBuilder("(new " + TypeUtils.toString(((IRType.MetaClass) node.type).componentType()) + "{");
        for (int i = 0; i < content.size(); i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(content.get(i));
        }
        result.append("})");

        return new GeneratedCode(node.type, result.toString());
    }

    @Override
    public GeneratedCode visit_Convert(final Convert node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "((" + TypeUtils.toString(node.type) + ")" + arg1 + ")");
    }

    @Override
    public GeneratedCode visit_InstanceOf(final InstanceOf node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 2, expressionStack, evaluationStack);

        final GeneratedCode arg2 = evaluationStack.pop();
        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + " instanceof " + arg2 + ")");
    }

    @Override
    public GeneratedCode visit_Truncate(final Truncate node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "((trunc " + TypeUtils.toString(node.type) + ")" + arg1 + ")");
    }

    @Override
    public GeneratedCode visit_Extend(final Extend node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return switch (node.extendType) {
            case SIGN -> new GeneratedCode(node.type, "((extend_sign " + TypeUtils.toString(node.type) + ")" + arg1 + ")");
            case ZERO -> new GeneratedCode(node.type, "((extend_zero " + TypeUtils.toString(node.type) + ")" + arg1 + ")");
        };
    }

    @Override
    public GeneratedCode visit_ClassInitialization(final ClassInitialization node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        emitArguments(node, 1, expressionStack, evaluationStack);

        final GeneratedCode arg1 = evaluationStack.pop();

        return new GeneratedCode(node.type, "(" + arg1 + ".$init$)");
    }

    @Override
    public GeneratedCode visit_InvokeInterface(final InvokeInterface node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        final List<Node> argumwens = node.arguments();
        for (final Node arg : argumwens) {
            emit(arg, expressionStack, evaluationStack);
        }

        List<GeneratedCode> content = new ArrayList<>();
        for (int i = 0; i < argumwens.size(); i++) {
            content.add(evaluationStack.pop());
        }

        content = content.reversed();

        final StringBuilder result = new StringBuilder("(");
        result.append(content.getFirst());
        result.append(".");
        result.append(node.name);
        result.append("(");

        for (int i = 1; i < content.size(); i++) {
            if (i > 1) {
                result.append(",");
            }
            result.append(content.get(i));
        }

        result.append("))");

        return new GeneratedCode(node.type, result.toString());
    }

    @Override
    public GeneratedCode visit_InvokeSpecial(final InvokeSpecial node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        final List<Node> argumwens = node.arguments();
        for (final Node arg : argumwens) {
            emit(arg, expressionStack, evaluationStack);
        }

        List<GeneratedCode> content = new ArrayList<>();
        for (int i = 0; i < argumwens.size(); i++) {
            content.add(evaluationStack.pop());
        }

        content = content.reversed();

        final StringBuilder result = new StringBuilder("(");
        result.append(content.getFirst());
        result.append(".");
        result.append(node.name);
        result.append("(");

        for (int i = 1; i < content.size(); i++) {
            if (i > 1) {
                result.append(",");
            }
            result.append(content.get(i));
        }

        result.append("))");

        return new GeneratedCode(node.type, result.toString());
    }

    @Override
    public GeneratedCode visit_InvokeVirtual(final InvokeVirtual node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        final List<Node> argumwens = node.arguments();
        for (final Node arg : argumwens) {
            emit(arg, expressionStack, evaluationStack);
        }

        List<GeneratedCode> content = new ArrayList<>();
        for (int i = 0; i < argumwens.size(); i++) {
            content.add(evaluationStack.pop());
        }

        content = content.reversed();

        final StringBuilder result = new StringBuilder("(");
        result.append(content.getFirst());
        result.append(".");
        result.append(node.name);
        result.append("(");

        for (int i = 1; i < content.size(); i++) {
            if (i > 1) {
                result.append(",");
            }
            result.append(content.get(i));
        }

        result.append("))");

        return new GeneratedCode(node.type, result.toString());
    }

    @Override
    public GeneratedCode visit_InvokeStatic(final InvokeStatic node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {

        final List<Node> argumwens = node.arguments();
        for (final Node arg : argumwens) {
            emit(arg, expressionStack, evaluationStack);
        }

        List<GeneratedCode> content = new ArrayList<>();
        for (int i = 0; i < argumwens.size(); i++) {
            content.add(evaluationStack.pop());
        }

        content = content.reversed();

        final StringBuilder result = new StringBuilder("(");
        result.append(content.getFirst());
        result.append(".");
        result.append(node.name);
        result.append("(");

        for (int i = 1; i < content.size(); i++) {
            if (i > 1) {
                result.append(",");
            }
            result.append(content.get(i));
        }

        result.append("))");

        return new GeneratedCode(node.type, result.toString());
    }

    @Override
    public GeneratedCode visit_NewMultiArray(final NewMultiArray node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        // TODO:
        return new GeneratedCode(node.type, "TODO");
    }

    @Override
    public GeneratedCode visit_InvokeDynamic(final InvokeDynamic node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public GeneratedCode visit_PHI(final PHI node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        // TODO:
        return new GeneratedCode(node.type, "TODO");
    }

    @Override
    public GeneratedCode visit_MethodType(final MethodType node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public GeneratedCode visit_MethodHandle(final MethodHandle node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public GeneratedCode visit_CaughtExceptionProjection(final CaughtExceptionProjection node, final Deque<Node> expressionStack, final Deque<GeneratedCode> evaluationStack) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void write(final ClassInitialization node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_ClassInitialization(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final Div node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_Div(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final Rem node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_Rem(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final ArrayLoad node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_ArrayLoad(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final InvokeSpecial node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_InvokeSpecial(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final InvokeInterface node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_InvokeInterface(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final InvokeVirtual node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_InvokeVirtual(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final InvokeStatic node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        final GeneratedCode generatedCode = visit_InvokeStatic(node, new ArrayDeque<>(), evaluationStack);

        emitWithTemporary(node, generatedCode, evaluationStack);
    }

    @Override
    public void write(final InvokeDynamic node) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void write(final LabelNode node) {
        writeIndentation();
        pw.print("// Label ");
        pw.println(node.label);
    }

    @Override
    public void write(final MergeNode node) {
        writeIndentation();
        pw.print("// Merge ");
        pw.println(node.label);
    }

    @Override
    public void write(final CheckCast node) {
        final Deque<GeneratedCode> evaluationStack = new ArrayDeque<>();

        emitArguments(node, 2, new ArrayDeque<>(), evaluationStack);

        if (evaluationStack.size() != 2) {
            throw new IllegalStateException("Expected exactly two values on the stack, but got " + evaluationStack.size());
        }

        final GeneratedCode arg1 = evaluationStack.pop();
        final GeneratedCode arg0 = evaluationStack.pop();
    }

    @Override
    public GeneratedCode emitTemporaryVariable(final String prefix, final GeneratedCode value) {
        final String varname = prefix + temporaryVariablesCounter++;

        writeIndentation();

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
        writeIndentation();
        pw.println(result.value);
    }

    @Override
    public String toString() {
        pw.flush();
        sw.flush();
        return sw.toString().trim().replace(System.lineSeparator(), "\n");
    }

    // CFG - Generation

    @Override
    public void begin(final Method method) {
        writeIndentation();
        pw.print("(method");

        for (final Value arg : method.methodArguments) {
            if (arg instanceof final Projection proj) {
                pw.print(" (");
                pw.print(TypeUtils.toString(arg.type));
                pw.print(" ");
                pw.print(proj.name());
                pw.print(")");
            } else throw new IllegalArgumentException("Unknown argument type " + arg.getClass());
        }
        pw.println();
        indentationLevel++;
    }

    @Override
    public void writeBreakTo(final String label, final Node currentNode, final Node targetNode) {
        writeIndentation();
        pw.print("break ");
        pw.print(label);
        pw.println();
    }

    @Override
    public void writeContinueTo(final String label, final Node currentNode, final Node targetNode) {
        writeIndentation();
        pw.print("continue ");
        pw.print(label);
        pw.println();
    }

    @Override
    public void finishBlock(final Sequencer.Block block) {
        indentationLevel--;
        writeIndentation();
        pw.println(")");
    }

    @Override
    public void startBlock(final Sequencer.Block b) {
        writeIndentation();
        pw.print("(block ");
        if (b.type == Sequencer.Block.Type.LOOP) {
            pw.print("loop ");
        }
        pw.print(b.label);
        pw.println();
        indentationLevel++;
    }

    @Override
    public void startIfWithTrueBlock(final If node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 1, new ArrayDeque<>(), stack);

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
        }

        writeIndentation();
        pw.print("(if ");
        pw.print(stack.pop().value);
        pw.println();

        indentationLevel++;
    }

    @Override
    public void startIfElseBlock(final If node) {
        indentationLevel--;
        writeIndentation();
        pw.println("(else)");

        indentationLevel++;
    }

    @Override
    public void finishIfBlock() {
        indentationLevel--;
        writeIndentation();
        pw.println(")");
    }

    @Override
    public void write(final Return node) {
        writeIndentation();
        pw.println("return");
    }

    @Override
    public void write(final ReturnValue node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 1, new ArrayDeque<>(), stack);

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
        }

        writeIndentation();
        pw.print("return ");
        pw.print(stack.pop().value);
        pw.println();
    }

    @Override
    public void write(final ArrayStore node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 3, new ArrayDeque<>(), stack);

        if (stack.size() != 3) {
            throw new IllegalStateException("Expected exactly three values on the stack, but got " + stack.size());
        }

        final GeneratedCode arg2 = stack.pop();
        final GeneratedCode arg1 = stack.pop();
        final GeneratedCode arg0 = stack.pop();

        writeIndentation();
        pw.print(arg0.value);
        pw.print("[");
        pw.print(arg1.value);
        pw.print("] = ");
        pw.print(arg2.value);
        pw.println();
    }

    @Override
    public void write(final PutStatic node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 2, new ArrayDeque<>(), stack);

        if (stack.size() != 2) {
            throw new IllegalStateException("Expected exactly two values on the stack, but got " + stack.size());
        }

        final GeneratedCode arg1 = stack.pop();
        final GeneratedCode arg0 = stack.pop();

        writeIndentation();
        pw.print(arg0.value);
        pw.print(".");
        pw.print(node.fieldName);
        pw.print(" = ");
        pw.print(arg1.value);
        pw.println();
    }

    @Override
    public void write(final PutField node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 2, new ArrayDeque<>(), stack);

        if (stack.size() != 2) {
            throw new IllegalStateException("Expected exactly two values on the stack, but got " + stack.size());
        }

        final GeneratedCode arg1 = stack.pop();
        final GeneratedCode arg0 = stack.pop();

        writeIndentation();
        pw.print(arg0.value);
        pw.print(".");
        pw.print(node.fieldName);
        pw.print(" = ");
        pw.print(arg1.value);
        pw.println();
    }

    @Override
    public void write(final MonitorEnter node) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void write(final MonitorExit node) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void writePreJump(final Node node) {

        for (final PHI phi : phiNodes) {
            final Node value = phi.initExpressionFor(node);
            if (value != null) {

                final GeneratedCode var = commitedTemporaryFor(phi);
                if (var == null) {
                    throw new IllegalStateException("No temporary variable for phi " + phi);
                }

                final Deque<GeneratedCode> stack = new ArrayDeque<>();
                emit(value, new ArrayDeque<>(), stack);

                if (stack.size() != 1) {
                    throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
                }

                final GeneratedCode arg0 = stack.pop();

                writeIndentation();
                pw.print(var.value);
                pw.print(" = ");
                pw.println(arg0.value);
            }
        }
    }

    @Override
    public void write(final Throw node) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void startLookupSwitch(final LookupSwitch node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 1, new ArrayDeque<>(), stack);

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
        }

        final GeneratedCode arg0 = stack.pop();

        writeIndentation();
        pw.print("lookupswitch(");
        pw.print(arg0.value);
        pw.println(") (");

        indentationLevel++;
    }

    @Override
    public void writeSwitchCase(final int key) {
        writeIndentation();
        pw.print("case(");
        pw.print(key);
        pw.println(") (");

        indentationLevel++;
    }

    @Override
    public void finishSwitchCase() {
        indentationLevel--;
        writeIndentation();

        pw.println(")");
    }

    @Override
    public void writeSwitchDefaultCase() {
        writeIndentation();

        pw.println("default (");

        indentationLevel++;
    }

    @Override
    public void finishSwitchDefault() {
        indentationLevel--;
        writeIndentation();

        pw.println(")");
    }

    @Override
    public void finishLookupSwitch() {
        indentationLevel--;
        writeIndentation();
        pw.println(")");
    }

    @Override
    public void startTableSwitch(final TableSwitch node) {
        final Deque<GeneratedCode> stack = new ArrayDeque<>();

        emitArguments(node, 1, new ArrayDeque<>(), stack);

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
        }

        final GeneratedCode arg0 = stack.pop();

        writeIndentation();
        pw.print("tableswitch(");
        pw.print(arg0.value);
        pw.print(",low=");
        pw.print(node.lowValue);
        pw.print(",high=");
        pw.print(node.highValue);
        pw.println(") (");

        indentationLevel++;
    }

    @Override
    public void startTableSwitchDefaultBlock() {
        writeIndentation();

        pw.println("default (");

        indentationLevel++;
    }

    @Override
    public void finishTableSwitchDefaultBlock() {
        indentationLevel--;
        writeIndentation();

        pw.println(")");
    }

    @Override
    public void finishTableSwitch() {
        indentationLevel--;
        writeIndentation();
        pw.println(")");
    }

    @Override
    public void startTryCatch(final ExceptionGuard node) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void startCatchBlock() {
    }

    @Override
    public void startCatchHandler(final List<IRType.MetaClass> exceptionTypes) {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void writeRethrowException() {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void finishCatchHandler() {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void finishTryCatch() {
        throw new IllegalStateException("Not supported by OpenCL!");
    }

    @Override
    public void finished() {
        indentationLevel--;
        writeIndentation();
        pw.println(")");
    }

    @Override
    public void writePHINodesFor(final Node node) {
        for (final Node defs : node.definitions()) {
            if (defs instanceof final PHI phi) {
                final Node initializedByThis = phi.initExpressionFor(node);
                if (initializedByThis == null) {
                    emitWithTemporary("phi", phi, new GeneratedCode(phi.type, "<<uninitialized>>"), new ArrayDeque<>());
                } else {
                    final Deque<GeneratedCode> stack = new ArrayDeque<>();
                    emit(initializedByThis, new ArrayDeque<>(), stack);

                    if (stack.size() != 1) {
                        throw new IllegalStateException("Expected exactly one value on the stack, but got " + stack.size());
                    }

                    final GeneratedCode arg0 = stack.pop();

                    emitWithTemporary("phi", phi, arg0, new ArrayDeque<>());
                }
                phiNodes.add(phi);
            }
        }
    }
}
