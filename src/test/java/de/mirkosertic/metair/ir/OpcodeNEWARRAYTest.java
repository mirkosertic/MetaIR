package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeKind;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OpcodeNEWARRAYTest {

    @Test
    public void test_NEWARRAY_byte() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.BYTE);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.bipush(10); // Value
            codeBuilder.bastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.iconst_1(); // Index
            codeBuilder.baload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_char() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.CHAR);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.bipush(10); // Value
            codeBuilder.castore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.iconst_1(); // Index
            codeBuilder.caload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_short() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.SHORT);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.bipush(10); // Value
            codeBuilder.sastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.sipush(2); // Index
            codeBuilder.saload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_int() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.INT);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.ldc(2); // Value
            codeBuilder.iastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.sipush(2); // Index
            codeBuilder.iaload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_long() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.LONG);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.ldc(2L); // Value
            codeBuilder.lastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.sipush(2); // Index
            codeBuilder.laload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_float() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.FLOAT);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.ldc(2.0f); // Value
            codeBuilder.fastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.sipush(2); // Index
            codeBuilder.faload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

    @Test
    public void test_NEWARRAY_double() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.newarray(TypeKind.DOUBLE);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.ldc(2.0d); // Value
            codeBuilder.dastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.sipush(2); // Index
            codeBuilder.daload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }

}