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

public class OpcodeLCONSTTest {

    @Test
    public void test_lconst_0() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.lconst_0();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveLong).map(t -> ((PrimitiveLong) t).value).toList()).containsExactly(0L);
    }

    @Test
    public void test_iconst_1() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> {
            methodBuilder.constantPool();
            methodBuilder.withCode(codeBuilder -> {
                codeBuilder.lconst_1();
                codeBuilder.pop();
                codeBuilder.return_();
            });
        }));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveLong).map(t -> ((PrimitiveLong) t).value).toList()).containsExactly(1L);
    }
}
