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
            codeBuilder.lstore(1);
            codeBuilder.lload(1);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(2);
            codeBuilder.lload(2);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(3);
            codeBuilder.lload(3);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(4);
            codeBuilder.lload(4);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(5);
            codeBuilder.lload(5);
            codeBuilder.pop();
            codeBuilder.lconst_1();
            codeBuilder.lstore(7);
            codeBuilder.lload(7);
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
