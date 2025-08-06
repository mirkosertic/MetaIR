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

public class OpcodeLLOADTest {

    @Test
    public void test_LLOAD() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.lconst_1();
            codeBuilder.lstore(0);
            codeBuilder.lload(0);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(2);
            codeBuilder.lload(2);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(4);
            codeBuilder.lload(4);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(6);
            codeBuilder.lload(6);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(8);
            codeBuilder.lload(8);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(10);
            codeBuilder.lload(10);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(12);
            codeBuilder.lload(12);
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
