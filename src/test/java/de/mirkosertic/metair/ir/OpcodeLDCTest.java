package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OpcodeLDCTest {

    @Test
    public void test_ldc_String() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.stringEntry("value"));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof StringConstant).map(t -> ((StringConstant) t).value).toList()).containsExactly("value");
    }

    @Test
    public void test_ldc_int() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.intEntry(1000));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(1000);
    }

    @Test
    public void test_ldc_long() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.longEntry(1000L));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveLong).map(t -> ((PrimitiveLong) t).value).toList()).containsExactly(1000L);
    }

    @Test
    public void test_ldc_float() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.floatEntry(1000.0f));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveFloat).map(t -> ((PrimitiveFloat) t).value).toList()).containsExactly(1000f);
    }

    @Test
    public void test_ldc_double() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.doubleEntry(1000.0d));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveDouble).map(t -> ((PrimitiveDouble) t).value).toList()).containsExactly(1000d);
    }

    @Test
    public void test_ldc_ClassRef() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            final ConstantPoolBuilder constantPool = methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.ldc(constantPool.classEntry(ConstantDescs.CD_String));
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof RuntimeclassReference).map(t -> ((RuntimeclassReference) t).type).toList()).containsExactly(ConstantDescs.CD_String);
    }
}
