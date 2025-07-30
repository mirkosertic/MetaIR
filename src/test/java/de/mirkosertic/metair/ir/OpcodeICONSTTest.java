package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OpcodeICONSTTest {

    @Test
    public void test_ICONST_0() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.iconst_0();
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(0);
    }

    @Test
    public void test_ICONST_1() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_1();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(1);
    }

    @Test
    public void test_ICONST_2() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_2();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(2);
    }

    @Test
    public void test_ICONST_3() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_3();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(3);
    }

    @Test
    public void test_ICONST_4() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_4();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(4);
    }

    @Test
    public void test_ICONST_5() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_5();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(5);
    }

    @Test
    public void test_ICONST_M1() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.iconst_m1();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveInt).map(t -> ((PrimitiveInt) t).value).toList()).containsExactly(-1);
    }

}
